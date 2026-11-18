package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.AvisoVisita
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.repository.ProgramadorAvisos
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SincronizarAvisosVisitasTest {
    private val zona = TimeZone.of("America/Argentina/Buenos_Aires")
    // 9 nov 22:30 en Buenos Aires: la fecha local de «ahora» es 2026-11-09.
    private val ahora = Instant.parse("2026-11-10T01:30:00Z")

    @Test
    fun programaLasVisitasFuturasConSuHoraLocalYRespetaLaSinHora() {
        val plan = planificarAvisos(
            listOf(
                visita("tarde", "2026-11-10", 13 * 60),
                visita("sinHora", "2026-11-11"),
                visita("masAdelante", "2026-11-17", 8 * 60),
                visita("vencidaHoy", "2026-11-09", 8 * 60),
                visita("vencidaAyer", "2026-11-08"),
                visita("sinFecha"),
                visita("sinFechaPausada", estado = EstadoObra.PAUSADA),
            ),
            programados = emptyMap(),
            ahora, zona,
        )

        assertEquals(listOf("tarde", "sinHora", "masAdelante"), plan.programar.map { it.obraId })
        // La visita sin hora dispara a las 9:00 locales de su fecha.
        assertEquals(Instant.parse("2026-11-11T12:00:00Z"), plan.programar.single { it.obraId == "sinHora" }.disparo)
        assertEquals(Instant.parse("2026-11-10T16:00:00Z"), plan.programar.single { it.obraId == "tarde" }.disparo)
        assertEquals(emptyList<String>(), plan.cancelar)
    }

    @Test
    fun dejaIgualLoYaProgramadoYReprogramaLoQueCambio() {
        val fijos = listOf(
            visita("sinCambio", "2026-11-11"),
            visita("reprogramada", "2026-11-11", 10 * 60),
            visita("vencio", "2026-11-09", 8 * 60),
            visita("eliminada"),
        )
        val programados = mapOf(
            "sinCambio" to Instant.parse("2026-11-11T12:00:00Z"),
            "reprogramada" to Instant.parse("2026-11-11T12:00:00Z"),
            "vencio" to Instant.parse("2026-11-09T11:00:00Z"),
            "eliminada" to Instant.parse("2026-11-12T12:00:00Z"),
        )
        val plan = planificarAvisos(fijos, programados, ahora, zona)

        // «reprogramada» movió su hora (13:00 UTC): se cancela el aviso viejo y se programa el nuevo.
        // «vencio» ya pasó su fecha y «eliminada» ya no está en la agenda: se cancelan.
        assertEquals(listOf("reprogramada"), plan.programar.map { it.obraId })
        assertEquals(Instant.parse("2026-11-11T13:00:00Z"), plan.programar.single().disparo)
        assertEquals(listOf("eliminada", "reprogramada", "vencio"), plan.cancelar.sorted())
    }

    @Test
    fun noProgramaUnaVisitaDeHoyConLaHoraYaPasada() {
        // Una visita de hoy sin hora dispara a las 9:00 locales (12:00 UTC), antes de «ahora».
        val plan = planificarAvisos(listOf(visita("hoySinHora", "2026-11-09")), programados = emptyMap(), ahora, zona)
        assertTrue(plan.programar.isEmpty())
    }

    @Test
    fun noTocaNadaCuandoLaAgendaConcuerdaConLoProgramado() {
        val agenda = listOf(visita("sinCambio", "2026-11-11"))
        val programados = mapOf("sinCambio" to disparoDe(agenda.single(), zona))
        val plan = planificarAvisos(agenda, programados, ahora, zona)
        assertTrue(plan.sinCambios)
    }

    @Test
    fun laSincronizacionAplicaElPlanEnElPuerto() = kotlinx.coroutines.test.runTest {
        val puerto = PuertoFalso()
        val sincronizar = SincronizarAvisosVisitas(puerto)
        // Primera vez: programa todos los avisos futuros de la agenda.
        val agendaVieja = listOf(visita("sinCambio", "2026-11-11"), visita("reprogramada", "2026-11-11"))
        sincronizar(agendaVieja, ahora, zona)
        assertEquals(setOf("sinCambio", "reprogramada"), puerto.programaciones.keys)
        assertTrue(puerto.cancelaciones.isEmpty())

        // La visita se reprogramó: cancela el aviso con la hora vieja y programa el nuevo.
        val agendaNueva = listOf(visita("sinCambio", "2026-11-11"), visita("reprogramada", "2026-11-11", 10 * 60))
        val disparoEsperado = disparoDe(agendaNueva.single { it.obra.id == "reprogramada" }, zona)
        sincronizar(agendaNueva, ahora, zona)
        assertEquals(disparoEsperado, puerto.programaciones.getValue("reprogramada").disparo)
        assertEquals(listOf("reprogramada"), puerto.cancelaciones.distinct())
    }

    private fun visita(id: String, fecha: String? = null, hora: Int? = null,
        estado: EstadoObra = EstadoObra.ACTIVA,
    ) = VisitaAgenda(
        obra(id, fecha, hora, estado),
        categoriaDe(fecha, hora, estado),
        fecha?.let(LocalDate::parse),
        hora,
    )

    private fun obra(id: String, fecha: String?, hora: Int?, estado: EstadoObra) = Obra(
        id, "usuario", id, "Cliente", "Dirección $id", false, estado,
        Instant.parse("2026-11-01T00:00:00Z"),
        proximaVisita = fecha?.let { Instant.parse("${it}T00:00:00Z") },
        horaVisita = hora,
    )

    /** Categorías equivalentes a las de [agendaVisitas] para las obras de estas pruebas. */
    private fun categoriaDe(fecha: String?, hora: Int?, estado: EstadoObra) = when {
        fecha == null -> if (estado == EstadoObra.ACTIVA) CategoriaAgendaVisita.SIN_PROGRAMAR else CategoriaAgendaVisita.VENCIDAS
        LocalDate.parse(fecha) < ahora.toLocalDateTime(zona).date -> CategoriaAgendaVisita.VENCIDAS
        hora != null && hora in 0 until 24 * 60 &&
            ahora > LocalDateTime(LocalDate.parse(fecha), LocalTime(hora / 60, hora % 60)).toInstant(zona) ->
            CategoriaAgendaVisita.VENCIDAS
        else -> CategoriaAgendaVisita.PROXIMAS
    }

    private class PuertoFalso : ProgramadorAvisos {
        val programaciones = mutableMapOf<String, AvisoVisita>()
        val cancelaciones = mutableListOf<String>()

        override suspend fun programados(): Map<String, Instant> = programaciones.mapValues { it.value.disparo }

        override suspend fun programar(aviso: AvisoVisita) {
            programaciones[aviso.obraId] = aviso
        }

        override suspend fun cancelar(obraId: String) {
            programaciones.remove(obraId)
            cancelaciones.add(obraId)
        }
    }
}
