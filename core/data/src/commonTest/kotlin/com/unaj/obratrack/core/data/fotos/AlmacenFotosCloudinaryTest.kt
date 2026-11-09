package com.unaj.obratrack.core.data.fotos

import com.unaj.obratrack.domain.repository.ResultadoSubidaFoto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlmacenFotosCloudinaryTest {
    private val config = CloudinaryConfig("nube", "preset_sin_firma")
    private val compresorChico = CompresorImagen { _, _, elegir -> elegir { ByteArray(10) } }

    private fun almacen(
        compresor: CompresorImagen = compresorChico,
        respuesta: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = AlmacenFotosCloudinary(config, HttpClient(MockEngine(respuesta)), compresor)

    @Test
    fun subeConLosCamposSinFirmaYDevuelveSecureUrl() = runTest {
        var pedido: HttpRequestData? = null
        var cuerpo = ""
        val resultado = almacen { request ->
            pedido = request
            cuerpo = request.body.toByteArray().decodeToString()
            respond("""{"public_id":"relevamientos/obra/n","secure_url":"https://res.cloudinary.com/nube/image/upload/v1/relevamientos/obra/n.jpg"}""")
        }.subir(byteArrayOf(1, 2), "relevamientos/obra", "n")

        assertEquals(ResultadoSubidaFoto.Exito("https://res.cloudinary.com/nube/image/upload/v1/relevamientos/obra/n.jpg"), resultado)
        assertEquals(HttpMethod.Post, pedido?.method)
        assertEquals("https://api.cloudinary.com/v1_1/nube/image/upload", pedido?.url.toString())
        for ((campo, valor) in listOf("upload_preset" to "preset_sin_firma", "folder" to "relevamientos/obra", "public_id" to "n")) {
            assertTrue(Regex("name=\"?$campo\"?[\\s\\S]*?\\r\\n\\r\\n$valor\\r\\n").containsMatchIn(cuerpo), campo)
        }
        assertTrue(cuerpo.contains("name=\"?file\"?".toRegex()))
        assertTrue(cuerpo.contains("image/jpeg"))
    }

    @Test
    fun respuestaDeErrorOSinUrlEsFallo() = runTest {
        val rechazada = almacen { respond("""{"error":{"message":"Upload preset not found"}}""", HttpStatusCode.BadRequest) }
        assertEquals(ResultadoSubidaFoto.Fallo, rechazada.subir(byteArrayOf(1), "c", "n"))
        val sinUrl = almacen { respond("""{"public_id":"n"}""") }
        assertEquals(ResultadoSubidaFoto.Fallo, sinUrl.subir(byteArrayOf(1), "c", "n"))
        val noJson = almacen { respond("<html>") }
        assertEquals(ResultadoSubidaFoto.Fallo, noJson.subir(byteArrayOf(1), "c", "n"))
    }

    @Test
    fun faltaDeRedYTimeoutSonSinConexion() = runTest {
        val sinRed = almacen { throw IOException("sin red") }
        assertEquals(ResultadoSubidaFoto.SinConexion, sinRed.subir(byteArrayOf(1), "c", "n"))
        val lenta = almacen { throw HttpRequestTimeoutException("https://api.cloudinary.com", 60_000) }
        assertEquals(ResultadoSubidaFoto.SinConexion, lenta.subir(byteArrayOf(1), "c", "n"))
    }

    @Test
    fun fotoQueNoBajaDelTopeNoSeSube() = runTest {
        var pedidos = 0
        val grande = almacen(compresor = { _, _, elegir -> elegir { ByteArray(PESO_MAXIMO_BYTES + 1) } }) { pedidos++; respond("{}") }
        assertEquals(ResultadoSubidaFoto.MuyGrande, grande.subir(byteArrayOf(1), "c", "n"))
        assertEquals(0, pedidos)
    }

    @Test
    fun imagenQueNoSePuedeLeerEsFallo() = runTest {
        val ilegible = almacen(compresor = { _, _, _ -> throw IllegalArgumentException("no es imagen") }) { respond("{}") }
        assertEquals(ResultadoSubidaFoto.Fallo, ilegible.subir(byteArrayOf(1), "c", "n"))
    }

    @Test
    fun laCancelacionSeRelanza() = runTest {
        val cancelada = almacen { throw CancellationException("cancelada") }
        assertFailsWith<CancellationException> { cancelada.subir(byteArrayOf(1), "c", "n") }
    }

    @Test
    fun prepararFotoBajaLaCalidadHastaElObjetivoYRespetaElTope() = runTest {
        val calidades = mutableListOf<Int>()
        var aperturas = 0
        val pesos = mapOf(85 to 3_000_000, 75 to 1_400_000)
        val lista = prepararFoto(byteArrayOf(1)) { _, lado, elegir ->
            aperturas++
            assertEquals(LADO_MAXIMO_PX, lado)
            elegir { calidad -> calidades += calidad; ByteArray(pesos.getValue(calidad)) }
        }
        assertEquals(listOf(85, 75), calidades)
        assertEquals(1, aperturas, "la imagen se decodifica una sola vez")
        assertEquals(1_400_000, lista?.size)

        val sobreObjetivo = prepararFoto(byteArrayOf(1)) { _, _, elegir -> elegir { calidad -> ByteArray(if (calidad == 40) 2_000_000 else 4_000_000) } }
        assertEquals(2_000_000, sobreObjetivo?.size)
        assertNull(prepararFoto(byteArrayOf(1)) { _, _, elegir -> elegir { ByteArray(PESO_MAXIMO_BYTES + 1) } })
    }

    @Test
    fun factorMuestreoLeeReducidoSinQuedarPorDebajoDelLadoMaximo() {
        assertEquals(1, factorMuestreo(1920, 1080, 1920))
        assertEquals(1, factorMuestreo(2850, 3800, 1920)) // a la mitad quedaría en 1900 px
        assertEquals(2, factorMuestreo(3000, 4000, 1920))
        assertEquals(4, factorMuestreo(8160, 6120, 1920)) // 50 MP: se lee a 2040 px
        assertEquals(1, factorMuestreo(640, 480, 1920))
    }

    @Test
    fun medidasReducidasConservanLaProporcion() {
        assertEquals(1920 to 1440, medidasReducidas(4080, 3060, 1920))
        assertEquals(1080 to 1920, medidasReducidas(2160, 3840, 1920))
        assertEquals(640 to 480, medidasReducidas(640, 480, 1920))
        assertEquals(1920 to 1, medidasReducidas(10_000, 2, 1920))
    }

    @Test
    fun urlMiniaturaPideUnRecorteCuadrado() {
        val base = "https://res.cloudinary.com/iwsier4h/image/upload/"
        assertEquals("${base}c_fill,w_160,h_160/v1/relevamientos/o/a.jpg", urlMiniaturaCloudinary("${base}v1/relevamientos/o/a.jpg"))
        for (igual in listOf("${base}c_fill,w_800/v1/a.jpg", "https://example.com/image/upload/a.jpg")) {
            assertEquals(igual, urlMiniaturaCloudinary(igual))
        }
    }

    @Test
    fun urlReducidaSoloTransformaFotosDeCloudinarySinTransformar() {
        val base = "https://res.cloudinary.com/iwsier4h/image/upload/"
        assertEquals("${base}c_fill,w_800/v1/relevamientos/o/a.jpg", urlReducidaCloudinary("${base}v1/relevamientos/o/a.jpg"))
        assertEquals("${base}c_fill,w_800/relevamientos/o/a.jpg", urlReducidaCloudinary("${base}relevamientos/o/a.jpg"))
        assertEquals("${base}c_fill,w_200/v1/a.jpg", urlReducidaCloudinary("${base}v1/a.jpg", 200))
        for (igual in listOf("${base}c_fill,w_800/v1/a.jpg", "${base}w_400/v1/a.jpg", "https://example.com/image/upload/a.jpg", "file:///foto.jpg")) {
            assertEquals(igual, urlReducidaCloudinary(igual))
        }
    }
}
