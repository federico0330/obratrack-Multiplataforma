package com.unaj.obratrack.core.firebase.tareas

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BajaTareaTest {
    @Test
    fun borrarElMasRecienteActualizaLaVisitaConElMayorRestante() = runTest {
        val almacen = AlmacenFalso(listOf("nuevo"), mapOf("viejo" to 10L, "medio" to 20L, "nuevo" to 30L))
        ejecutarBajaTarea(almacen, true)
        assertEquals(PlanBajaTarea(listOf("nuevo"), true, true, 20L), almacen.aplicado)
        assertEquals(1, almacen.escrituras)
    }

    @Test
    fun borrarTodosQuitaLaUltimaVisita() = runTest {
        val almacen = AlmacenFalso(listOf("unico"), mapOf("unico" to 30L))
        ejecutarBajaTarea(almacen, true)
        assertTrue(almacen.aplicado!!.actualizarUltimaVisita)
        assertNull(almacen.aplicado!!.ultimaVisitaMillis)
    }

    @Test
    fun desvincularConservaRelevamientosYNoConsultaNiCambiaLaVisita() = runTest {
        val almacen = AlmacenFalso(listOf("unico"), mapOf("unico" to 30L))
        ejecutarBajaTarea(almacen, false)
        assertEquals(PlanBajaTarea(listOf("unico"), false, false, null), almacen.aplicado)
        assertEquals(0, almacen.lecturasFechas)
    }

    @Test
    fun sinVinculadosSoloBorraLaTarea() = runTest {
        val almacen = AlmacenFalso()
        ejecutarBajaTarea(almacen, true)
        assertFalse(almacen.aplicado!!.actualizarUltimaVisita)
        assertEquals(0, almacen.lecturasFechas)
    }

    @Test
    fun aceptaElLimiteExactoDeCadaModo() = runTest {
        val borrar = AlmacenFalso((1..498).map(Int::toString))
        ejecutarBajaTarea(borrar, true)
        assertEquals(1, borrar.escrituras)
        val desvincular = AlmacenFalso((1..499).map(Int::toString))
        ejecutarBajaTarea(desvincular, false)
        assertEquals(1, desvincular.escrituras)
    }

    @Test
    fun demasiadosVinculadosNoProducenEscriturasParciales() = runTest {
        for ((cantidad, eliminar) in listOf(499 to true, 500 to false)) {
            val almacen = AlmacenFalso((1..cantidad).map(Int::toString))
            assertFailsWith<DemasiadosVinculadosException> { ejecutarBajaTarea(almacen, eliminar) }
            assertEquals(0, almacen.escrituras)
            assertEquals(0, almacen.lecturasFechas)
        }
    }

    @Test
    fun falloDeLecturaImpideCualquierEscritura() = runTest {
        val almacen = AlmacenFalso(listOf("1")).apply { falloLectura = IllegalStateException("sin red") }
        assertFailsWith<IllegalStateException> { ejecutarBajaTarea(almacen, true) }
        assertEquals(0, almacen.escrituras)
    }

    @Test
    fun falloDeCommitNoSeConvierteEnExitoNiSeReintentaPorPartes() = runTest {
        val almacen = AlmacenFalso(listOf("1")).apply { falloCommit = IllegalStateException("permiso") }
        assertFailsWith<IllegalStateException> { ejecutarBajaTarea(almacen, true) }
        assertEquals(1, almacen.escrituras)
        assertNull(almacen.aplicado)
    }

    @Test
    fun cancelacionSePropagaSinEscribir() = runTest {
        val almacen = AlmacenFalso(listOf("1")).apply { falloLectura = CancellationException() }
        assertFailsWith<CancellationException> { ejecutarBajaTarea(almacen, true) }
        assertEquals(0, almacen.escrituras)
    }
}

private class AlmacenFalso(
    private val vinculados: List<String> = emptyList(),
    private val fechas: Map<String, Long> = emptyMap(),
) : AlmacenBajaTarea {
    var aplicado: PlanBajaTarea? = null
    var escrituras = 0
    var lecturasFechas = 0
    var falloLectura: Exception? = null
    var falloCommit: Exception? = null
    override suspend fun leerVinculados() = vinculados
    override suspend fun leerFechas(): Map<String, Long> {
        lecturasFechas++
        falloLectura?.let { throw it }
        return fechas
    }
    override suspend fun aplicar(plan: PlanBajaTarea) {
        escrituras++
        falloCommit?.let { throw it }
        aplicado = plan
    }
}
