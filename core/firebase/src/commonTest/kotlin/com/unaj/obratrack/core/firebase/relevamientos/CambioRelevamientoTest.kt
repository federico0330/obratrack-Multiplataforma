package com.unaj.obratrack.core.firebase.relevamientos

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CambioRelevamientoTest {
    @Test
    fun moverElUltimoHaciaAtrasTomaLaFechaMayorRestante() = runTest {
        val tx = CambioFalso()
        prepararEdicion("uid", "ultimo", RelevamientoUpdateFields(5, "Editado", null), tx)
        assertEquals(20L, tx.fechaGuardada)
        assertEquals(5L, tx.editado?.fechaMillis)
    }

    @Test
    fun moverOtroHaciaAdelanteActualizaLaFechaMayor() = runTest {
        val tx = CambioFalso()
        prepararEdicion("uid", "otro", RelevamientoUpdateFields(40, "Editado", "tarea"), tx)
        assertEquals(40L, tx.fechaGuardada)
        assertEquals("tarea", tx.editado?.tareaId)
        assertEquals(listOf("obra", "registro", "tarea", "fechas"), tx.lecturas)
    }

    @Test
    fun editarElUnicoUsaSuNuevaFecha() = runTest {
        val tx = CambioFalso().apply { fechas = mapOf("ultimo" to 30) }
        prepararEdicion("uid", "ultimo", RelevamientoUpdateFields(5, "Editado", null), tx)
        assertEquals(5L, tx.fechaGuardada)
    }

    @Test
    fun borrarElMasRecienteConservaLaMayorFechaQueQueda() = runTest {
        val tx = CambioFalso()
        prepararBaja("uid", "ultimo", tx)
        assertEquals(20L, tx.fechaGuardada)
        assertEquals(1, tx.bajas)
    }

    @Test
    fun borrarElUnicoQuitaLaUltimaVisita() = runTest {
        val tx = CambioFalso().apply { fechas = mapOf("ultimo" to 30) }
        prepararBaja("uid", "ultimo", tx)
        assertNull(tx.fechaGuardada)
        assertEquals(1, tx.bajas)
    }

    @Test
    fun noEditaNiBorraRegistrosInexistentesOAjenos() = runTest {
        for (dueno in listOf(null, "otro")) {
            val tx = CambioFalso().apply { duenoRegistro = dueno }
            if (dueno == null) assertFailsWith<RelevamientoNoEncontradoException> { prepararBaja("uid", "ultimo", tx) }
            else assertFailsWith<ObraNoAutorizadaException> { prepararBaja("uid", "ultimo", tx) }
            assertEquals(0, tx.bajas)
            assertNull(tx.editado)
        }
        val ajena = CambioFalso().apply { duenoObra = "otro" }
        assertFailsWith<ObraNoAutorizadaException> { prepararEdicion("uid", "ultimo", RelevamientoUpdateFields(20, "Nota", null), ajena) }
        assertNull(ajena.editado)
    }

    @Test
    fun tareaInexistenteOAjenasImpidenTodaEscritura() = runTest {
        for (dueno in listOf(null, "otro")) {
            val tx = CambioFalso().apply { duenoTarea = dueno }
            assertFailsWith<TareaNoDisponibleException> {
                prepararEdicion("uid", "ultimo", RelevamientoUpdateFields(20, "Nota", "tarea"), tx)
            }
            assertNull(tx.editado)
            assertEquals(listOf("obra", "registro", "tarea"), tx.lecturas)
        }
    }

    @Test
    fun falloOCancelacionDeConsultaNoGeneranBajasParciales() = runTest {
        val tx = CambioFalso().apply { fallo = CancellationException() }
        assertFailsWith<CancellationException> { prepararBaja("uid", "ultimo", tx) }
        assertEquals(0, tx.bajas)
    }
}

private class CambioFalso : TransaccionCambioRelevamiento {
    var duenoObra: String? = "uid"
    var duenoRegistro: String? = "uid"
    var duenoTarea: String? = "uid"
    var fechas = mapOf("otro" to 20L, "ultimo" to 30L)
    val lecturas = mutableListOf<String>()
    var editado: RelevamientoUpdateFields? = null
    var fechaGuardada: Long? = null
    var bajas = 0
    var fallo: Exception? = null
    override suspend fun leerDuenoObra() = duenoObra.also { lecturas += "obra" }
    override suspend fun leerDuenoRelevamiento() = duenoRegistro.also { lecturas += "registro" }
    override suspend fun leerDuenoTarea(id: String) = duenoTarea.also { lecturas += "tarea" }
    override suspend fun leerFechas(): Map<String, Long> {
        lecturas += "fechas"
        fallo?.let { throw it }
        return fechas
    }
    override fun actualizar(fields: RelevamientoUpdateFields, ultimaVisitaMillis: Long) {
        editado = fields
        fechaGuardada = ultimaVisitaMillis
    }
    override fun eliminar(ultimaVisitaMillis: Long?) {
        bajas++
        fechaGuardada = ultimaVisitaMillis
    }
}
