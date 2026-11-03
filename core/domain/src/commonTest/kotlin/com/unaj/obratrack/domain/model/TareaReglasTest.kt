package com.unaj.obratrack.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TareaReglasTest {
    private val hoy = LocalDate(2026, 11, 2)

    private fun tarea(
        titulo: String = "T",
        estado: EstadoTarea = EstadoTarea.PENDIENTE,
        inicio: LocalDate? = null,
        fin: LocalDate? = null,
    ) = Tarea("id-$titulo", "obra", "uid", titulo, fechaInicio = inicio, fechaFin = fin, estado = estado)

    @Test
    fun estaPlanificadaRequiereAmbasFechas() {
        assertTrue(tarea(inicio = hoy, fin = hoy).estaPlanificada())
        assertFalse(tarea(inicio = hoy).estaPlanificada())
        assertFalse(tarea().estaPlanificada())
    }

    @Test
    fun atrasadaSoloSiElFinYaPasoYNoEstaCompletada() {
        val ayer = LocalDate(2026, 11, 1)
        assertTrue(tarea(fin = ayer).estaAtrasada(hoy))
        assertTrue(tarea(estado = EstadoTarea.EN_CURSO, fin = ayer).estaAtrasada(hoy))
        assertFalse(tarea(estado = EstadoTarea.COMPLETADA, fin = ayer).estaAtrasada(hoy))
        assertFalse(tarea().estaAtrasada(hoy))
    }

    @Test
    fun conFinHoyTodaviaNoEstaAtrasada() {
        assertFalse(tarea(fin = hoy).estaAtrasada(hoy))
    }

    @Test
    fun ordenaPorInicioConSinFechaAlFinalYDesempataPorTitulo() {
        val a = tarea("beta", inicio = LocalDate(2026, 11, 1), fin = LocalDate(2026, 11, 3))
        val b = tarea("Alfa", inicio = LocalDate(2026, 11, 1), fin = LocalDate(2026, 11, 3))
        val c = tarea("gamma", inicio = LocalDate(2026, 10, 1), fin = LocalDate(2026, 10, 2))
        val d = tarea("Zeta")

        assertEquals(listOf(c, b, a, d), listOf(d, a, b, c).ordenadas())
    }

    @Test
    fun resumenCuentaAtrasadasTambienComoPendientes() {
        val ayer = LocalDate(2026, 11, 1)
        val tareas = listOf(
            tarea("a", estado = EstadoTarea.COMPLETADA, fin = ayer),
            tarea("b", fin = ayer),
            tarea("c", estado = EstadoTarea.EN_CURSO),
        )

        assertEquals(ResumenTareas(completadas = 1, pendientes = 2, atrasadas = 1), tareas.resumen(hoy))
    }
}
