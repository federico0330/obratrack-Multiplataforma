package com.unaj.obratrack.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ResumenObrasTest {
    private fun obra(id: String, estado: EstadoObra) = Obra(
        id = id,
        userId = "uid-1",
        nombre = "Obra $id",
        cliente = "Cliente",
        direccion = "Calle 1",
        publica = false,
        estado = estado,
        fechaInicio = Instant.parse("2026-11-01T00:00:00Z"),
    )

    @Test
    fun listaVaciaDaTodoEnCero() {
        assertEquals(ResumenObras(total = 0, activas = 0, pausadas = 0, finalizadas = 0), resumenObras(emptyList()))
    }

    @Test
    fun unSoloEstadoSumaSoloEnEseEstado() {
        val obras = listOf(obra("1", EstadoObra.PAUSADA), obra("2", EstadoObra.PAUSADA), obra("3", EstadoObra.PAUSADA))

        assertEquals(ResumenObras(total = 3, activas = 0, pausadas = 3, finalizadas = 0), resumenObras(obras))
    }

    @Test
    fun mezclaDeEstadosSeDesglosaYElTotalCoincideConLaSuma() {
        val obras = listOf(
            obra("1", EstadoObra.ACTIVA),
            obra("2", EstadoObra.ACTIVA),
            obra("3", EstadoObra.PAUSADA),
            obra("4", EstadoObra.FINALIZADA),
            obra("5", EstadoObra.ACTIVA),
        )

        val resumen = resumenObras(obras)

        assertEquals(ResumenObras(total = 5, activas = 3, pausadas = 1, finalizadas = 1), resumen)
        assertEquals(resumen.total, resumen.activas + resumen.pausadas + resumen.finalizadas)
    }
}
