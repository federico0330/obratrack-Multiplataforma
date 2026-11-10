package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AgendaVisitasTest {
    private val zona = TimeZone.of("America/Argentina/Buenos_Aires")
    private val ahora = Instant.parse("2026-11-10T01:30:00Z")

    @Test
    fun conservaCategoriasYOrdenDelNativo() {
        val obras = listOf(
            obra("sinReciente", ultima = "2026-11-08"),
            obra("masAdelante", "2026-11-17"),
            obra("tarde", "2026-11-16", 15 * 60),
            obra("sinHora", "2026-11-16"),
            obra("temprano", "2026-11-16", 8 * 60, EstadoObra.PAUSADA),
            obra("hoy", "2026-11-09"),
            obra("vencidaHoy", "2026-11-09", 22 * 60),
            obra("vencidaAyer", "2026-11-08"),
            obra("sinAntigua"),
            obra("pausadaSinFecha", estado = EstadoObra.PAUSADA),
        )
        val agenda = agendaVisitas(obras, ahora, zona)

        assertEquals(listOf("vencidaAyer", "vencidaHoy", "hoy", "temprano", "sinHora", "tarde",
            "masAdelante", "sinAntigua", "sinReciente"), agenda.map { it.obra.id })
        assertEquals(listOf(2, 1, 3, 1, 2), agenda.groupBy { it.categoria }.values.map { it.size })
        assertEquals(CategoriaAgendaVisita.entries, agenda.map { it.categoria }.distinct())
        assertEquals(listOf(8L, 1L), agenda.takeLast(2).map { it.diasSinVisita })
    }

    @Test
    fun evaluaLaHoraLocalYLasFechasSinHoraVencenAlCambiarElDia() {
        val obras = listOf(obra("conHora", "2026-11-09", 22 * 60 + 30), obra("horaInvalida", "2026-11-09", 1440))
        val aTiempo = agendaVisitas(obras, ahora, zona)

        assertEquals(listOf(CategoriaAgendaVisita.HOY, CategoriaAgendaVisita.HOY), aTiempo.map { it.categoria })
        assertNull(aTiempo.single { it.obra.id == "horaInvalida" }.horaMinutos)
        assertEquals(CategoriaAgendaVisita.VENCIDAS, agendaVisitas(obras, Instant.parse("2026-11-10T01:30:01Z"), zona).first().categoria)
        assertEquals(listOf(CategoriaAgendaVisita.VENCIDAS, CategoriaAgendaVisita.VENCIDAS),
            agendaVisitas(obras, Instant.parse("2026-11-10T03:00:00Z"), zona).map { it.categoria })
    }

    private fun obra(id: String, fecha: String? = null, hora: Int? = null,
        estado: EstadoObra = EstadoObra.ACTIVA, ultima: String? = null,
    ) = Obra(id, "usuario", id, "Cliente", "Dirección", false, estado,
        Instant.parse("2026-11-01T00:00:00Z"), ultimaVisita = ultima?.let(::fechaUtc),
        proximaVisita = fecha?.let(::fechaUtc), horaVisita = hora)

    private fun fechaUtc(fecha: String) = Instant.parse("${fecha}T00:00:00Z")
}
