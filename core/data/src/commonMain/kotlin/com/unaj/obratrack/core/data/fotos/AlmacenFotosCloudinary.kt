package com.unaj.obratrack.core.data.fotos

import com.unaj.obratrack.domain.repository.AlmacenFotos
import com.unaj.obratrack.domain.repository.ResultadoSubidaFoto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Cuenta de Cloudinary y preset de subida sin firma. El preset es público por diseño. */
data class CloudinaryConfig(val cloudName: String, val uploadPreset: String)

/** Sube fotos a Cloudinary sin firma: no hace falta servidor propio ni plan pago de Firebase. */
class AlmacenFotosCloudinary(
    private val config: CloudinaryConfig,
    private val client: HttpClient,
    private val compresor: CompresorImagen,
) : AlmacenFotos {
    override suspend fun subir(bytes: ByteArray, carpeta: String, nombre: String): ResultadoSubidaFoto = try {
        // Achicar la foto es pesado: no va en el hilo principal.
        val foto = withContext(Dispatchers.Default) { prepararFoto(bytes, compresor) }
        if (foto == null) {
            ResultadoSubidaFoto.MuyGrande
        } else {
            val respuesta = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/${config.cloudName}/image/upload",
                formData = formData {
                    append("upload_preset", config.uploadPreset)
                    append("folder", carpeta)
                    append("public_id", nombre)
                    append("file", foto, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$nombre.jpg\"")
                    })
                },
            )
            val url = if (respuesta.status.isSuccess()) leerSecureUrl(respuesta.bodyAsText()) else null
            url?.let(ResultadoSubidaFoto::Exito) ?: ResultadoSubidaFoto.Fallo
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        // Sin red, sin DNS o sin respuesta a tiempo (los timeouts de Ktor también son IOException).
        ResultadoSubidaFoto.SinConexion
    } catch (_: Exception) {
        ResultadoSubidaFoto.Fallo
    }
}

internal fun leerSecureUrl(cuerpo: String): String? =
    runCatching { Json.parseToJsonElement(cuerpo).jsonObject["secure_url"]?.jsonPrimitive?.contentOrNull }
        .getOrNull()?.takeIf { it.startsWith("https://") }

/** Cliente con el motor de cada plataforma (OkHttp en Android, Darwin en iOS) y su compresor de imágenes. */
fun crearAlmacenFotosCloudinary(config: CloudinaryConfig): AlmacenFotos = AlmacenFotosCloudinary(
    config = config,
    client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
        }
    },
    compresor = compresorPlataforma,
)
