package com.unaj.obratrack.core.data.sugerencias

import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Arma la respuesta de Gemini con [json] como texto de la primera parte del primer candidato. */
private fun respuestaGemini(json: String, finishReason: String? = "STOP"): String = buildJsonObject {
    putJsonArray("candidates") {
        add(
            buildJsonObject {
                putJsonObject("content") {
                    putJsonArray("parts") { add(buildJsonObject { put("text", json) }) }
                    put("role", "model")
                }
                if (finishReason != null) put("finishReason", finishReason)
            },
        )
    }
}.toString()

private fun tareasJson(vararg tareas: Pair<String, String>) =
    """{"tareas":[${tareas.joinToString(",") { """{"titulo":"${it.first}","categoria":"${it.second}"}""" }}]}"""

private fun sugeridorCon(
    config: GeminiConfig = GeminiConfig("clave-secreta", "modelo-x"),
    manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = SugeridorConGemini(config, HttpClient(MockEngine(manejador)))

private fun MockRequestHandleScope.ok(cuerpo: String) =
    respond(cuerpo, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

class SugeridorConGeminiTest {
    @Test
    fun enviaLaClaveEnElEncabezadoYNoEnLaUrl() = runTest {
        var pedido: HttpRequestData? = null
        val sugeridor = sugeridorCon { request ->
            pedido = request
            ok(respuestaGemini(tareasJson("Excavación" to "CIMIENTOS_Y_EXCAVACION")))
        }

        sugeridor.sugerir("Casa de dos plantas")

        val request = pedido!!
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("clave-secreta", request.headers["x-goog-api-key"])
        assertTrue(request.url.toString().endsWith("/v1beta/models/modelo-x:generateContent"))
        assertFalse(request.url.toString().contains("clave-secreta"))
        assertTrue(request.url.parameters.isEmpty())
    }

    @Test
    fun elCuerpoLlevaLaDescripcionElEsquemaYLasCategoriasPermitidas() = runTest {
        var cuerpo: String? = null
        val sugeridor = sugeridorCon { request ->
            cuerpo = (request.body as TextContent).text
            ok(respuestaGemini(tareasJson("Excavación" to "CIMIENTOS_Y_EXCAVACION")))
        }

        sugeridor.sugerir("Casa de dos plantas con quincho")

        val raiz = Json.parseToJsonElement(cuerpo!!).jsonObject
        val usuario = raiz["contents"]!!.jsonArray.single().jsonObject
        assertEquals("Casa de dos plantas con quincho", usuario["parts"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content)
        val config = raiz["generationConfig"]!!.jsonObject
        assertEquals("application/json", config["responseMimeType"]!!.jsonPrimitive.content)
        val categorias = config["responseSchema"]!!.jsonObject["properties"]!!.jsonObject["tareas"]!!.jsonObject["items"]!!
            .jsonObject["properties"]!!.jsonObject["categoria"]!!.jsonObject["enum"]!!.jsonArray
        assertEquals(CategoriaTarea.entries.map { it.name }, categorias.map { it.jsonPrimitive.content })
        assertTrue(raiz["systemInstruction"] is JsonObject)
    }

    @Test
    fun devuelveLasTareasConTituloLibreYSuCategoria() = runTest {
        val sugeridor = sugeridorCon {
            ok(respuestaGemini(tareasJson("Excavación de cimientos" to "CIMIENTOS_Y_EXCAVACION", "Tendido eléctrico" to "ELECTRICIDAD")))
        }

        val resultado = sugeridor.sugerir("Casa")

        assertEquals(
            ResultadoSugerir.Exito(
                listOf(
                    SugerenciaTarea(TituloSugerido.Libre("Excavación de cimientos"), CategoriaTarea.CIMIENTOS_Y_EXCAVACION),
                    SugerenciaTarea(TituloSugerido.Libre("Tendido eléctrico"), CategoriaTarea.ELECTRICIDAD),
                ),
            ),
            resultado,
        )
    }

    @Test
    fun categoriaDesconocidaPasaAOtrosYSeAceptaEnMinusculas() = runTest {
        val sugeridor = sugeridorCon { ok(respuestaGemini(tareasJson("A" to "INVENTADA", "B" to "pintura"))) }

        val sugerencias = assertIs<ResultadoSugerir.Exito>(sugeridor.sugerir("Casa")).sugerencias

        assertEquals(listOf(CategoriaTarea.OTROS, CategoriaTarea.PINTURA), sugerencias.map { it.categoria })
    }

    @Test
    fun descartaVaciosYRepetidosYRecortaLosTitulosLargos() = runTest {
        val largo = "x".repeat(200)
        val sugeridor = sugeridorCon {
            ok(respuestaGemini(tareasJson("  " to "OTROS", "Pintura" to "PINTURA", "pintura" to "PINTURA", largo to "OTROS")))
        }

        val sugerencias = assertIs<ResultadoSugerir.Exito>(sugeridor.sugerir("Casa")).sugerencias

        assertEquals(2, sugerencias.size)
        assertEquals("Pintura", (sugerencias[0].titulo as TituloSugerido.Libre).texto)
        assertEquals(MAX_LARGO_TITULO, (sugerencias[1].titulo as TituloSugerido.Libre).texto.length)
    }

    @Test
    fun noDevuelveMasDeLasTareasMaximas() = runTest {
        val muchas = (1..30).map { "Tarea $it" to "OTROS" }.toTypedArray()
        val sugeridor = sugeridorCon { ok(respuestaGemini(tareasJson(*muchas))) }

        val sugerencias = assertIs<ResultadoSugerir.Exito>(sugeridor.sugerir("Casa")).sugerencias

        assertEquals(MAX_TAREAS_SUGERIDAS, sugerencias.size)
        assertEquals("Tarea 1", (sugerencias.first().titulo as TituloSugerido.Libre).texto)
    }

    @Test
    fun el429EsLimiteAlcanzado() = runTest {
        val sugeridor = sugeridorCon { respond("{}", HttpStatusCode.TooManyRequests) }

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.LIMITE_ALCANZADO), sugeridor.sugerir("Casa"))
    }

    @Test
    fun otrosErroresDelServidorSonDesconocidos() = runTest {
        listOf(HttpStatusCode.BadRequest, HttpStatusCode.Forbidden, HttpStatusCode.InternalServerError, HttpStatusCode.ServiceUnavailable)
            .forEach { estado ->
                val sugeridor = sugeridorCon { respond("{}", estado) }

                assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.DESCONOCIDO), sugeridor.sugerir("Casa"), "$estado")
            }
    }

    @Test
    fun sinRedEsSinConexion() = runTest {
        val sugeridor = sugeridorCon { throw IOException("sin red") }

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.SIN_CONEXION), sugeridor.sugerir("Casa"))
    }

    @Test
    fun otraExcepcionEsDesconocida() = runTest {
        val sugeridor = sugeridorCon { error("algo raro") }

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.DESCONOCIDO), sugeridor.sugerir("Casa"))
    }

    @Test
    fun respuestasMalFormadasSonInvalidasSinLanzar() = runTest {
        val cuerpos = listOf(
            "no es json",
            "[]",
            "{}",
            """{"candidates":[]}""",
            respuestaGemini("esto no es json"),
            respuestaGemini("""{"otra":1}"""),
            respuestaGemini("""{"tareas":[]}"""),
            respuestaGemini("""{"tareas":[{"titulo":"","categoria":"OTROS"},{"categoria":"OTROS"},7]}"""),
            respuestaGemini(tareasJson("A" to "OTROS"), finishReason = "MAX_TOKENS"),
            respuestaGemini(tareasJson("A" to "OTROS"), finishReason = "SAFETY"),
            """{"promptFeedback":{"blockReason":"SAFETY"}}""",
        )
        cuerpos.forEach { cuerpo ->
            val sugeridor = sugeridorCon { ok(cuerpo) }

            assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.RESPUESTA_INVALIDA), sugeridor.sugerir("Casa"), cuerpo)
        }
    }

    @Test
    fun laConfiguracionNoMuestraLaClaveAlImprimirse() {
        val texto = GeminiConfig("clave-secreta", "modelo-x").toString()

        assertFalse(texto.contains("clave-secreta"))
        assertTrue(texto.contains("modelo-x"))
    }

    @Test
    fun elModeloPorDefectoEsElDeLaConfiguracion() {
        assertEquals(GeminiConfig.MODELO_POR_DEFECTO, GeminiConfig("k").modelo)
    }
}
