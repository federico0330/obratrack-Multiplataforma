package com.unaj.obratrack.core.firebase.relevamientos

import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AltaRelevamientoTest {
    private val dto = RelevamientoDto("nuevo", "obra", "uid", 20, "Avance")

    @Test
    fun altaSinTareaNoConsultaTareasYAvanzaLaVisita() = runTest {
        val tx = TransaccionFalsa()
        prepararAlta(dto, tx)
        assertEquals(dto to 20L, tx.escritura)
        assertNull(tx.tareaConsultada)
    }

    @Test
    fun fechaAnteriorNoHaceRetrocederLaUltimaVisita() = runTest {
        val tx = TransaccionFalsa().apply { obra = ObraParaRelevamiento("uid", 30) }
        prepararAlta(dto, tx)
        assertEquals(30L, tx.escritura?.second)
    }

    @Test
    fun vinculoVerificaElDuenoDeLaTareaAntesDeEscribir() = runTest {
        val tx = TransaccionFalsa()
        prepararAlta(dto.copy(tareaId = "tarea"), tx)
        assertEquals("tarea", tx.tareaConsultada)
        assertEquals("tarea", tx.escritura?.first?.tareaId)
    }

    @Test
    fun obraInexistenteOAjenaNoGeneraEscrituras() = runTest {
        for (obra in listOf(null, ObraParaRelevamiento("otro", null))) {
            val tx = TransaccionFalsa().apply { this.obra = obra }
            assertFailsWith<ObraNoAutorizadaException> { prepararAlta(dto, tx) }
            assertNull(tx.escritura)
        }
    }

    @Test
    fun tareaInexistenteOAjenaNoGeneraEscrituras() = runTest {
        for (dueno in listOf(null, "otro")) {
            val tx = TransaccionFalsa().apply { duenoTarea = dueno }
            assertFailsWith<TareaNoDisponibleException> { prepararAlta(dto.copy(tareaId = "tarea"), tx) }
            assertNull(tx.escritura)
        }
    }

    @Test
    fun falloOCancelacionDeLecturaNoSeConviertenEnEscrituras() = runTest {
        val tx = TransaccionFalsa().apply { fallo = CancellationException() }
        assertFailsWith<CancellationException> { prepararAlta(dto, tx) }
        assertNull(tx.escritura)
    }

    @Test
    fun payloadUsaTimestampYOmitirVinculoNoEscribeUnIdVacio() {
        val campos = dto.camposAlta()
        assertEquals(setOf("userId", "fecha", "nota"), campos.keys)
        assertEquals(20.0, (campos["fecha"] as Timestamp).toMilliseconds())
        assertEquals("tarea", dto.copy(tareaId = "tarea").camposAlta()["tareaId"])
    }
}

private class TransaccionFalsa : TransaccionAltaRelevamiento {
    var obra: ObraParaRelevamiento? = ObraParaRelevamiento("uid", null)
    var duenoTarea: String? = "uid"
    var escritura: Pair<RelevamientoDto, Long>? = null
    var tareaConsultada: String? = null
    var fallo: Exception? = null
    override suspend fun leerObra(): ObraParaRelevamiento? {
        fallo?.let { throw it }
        return obra
    }
    override suspend fun leerDuenoTarea(tareaId: String): String? {
        tareaConsultada = tareaId
        return duenoTarea
    }
    override fun guardar(dto: RelevamientoDto, ultimaVisitaMillis: Long) { escritura = dto to ultimaVisitaMillis }
}
