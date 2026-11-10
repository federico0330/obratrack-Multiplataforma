package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class CategoriaAgendaVisita { VENCIDAS, HOY, ESTA_SEMANA, PROXIMAS, SIN_PROGRAMAR }

data class VisitaAgenda(
    val obra: Obra,
    val categoria: CategoriaAgendaVisita,
    val fecha: LocalDate? = null,
    val horaMinutos: Int? = null,
    val diasSinVisita: Long? = null,
)

private const val HORA_POR_DEFECTO = 9 * 60

/** Las fechas guardadas son días de calendario en UTC. El vencimiento se evalúa en la zona local. */
fun agendaVisitas(obras: List<Obra>, ahora: Instant, zona: TimeZone): List<VisitaAgenda> {
    val hoy = ahora.toLocalDateTime(zona).date
    return obras.mapNotNull { obra ->
        val fecha = obra.proximaVisita?.toLocalDateTime(TimeZone.UTC)?.date
        if (fecha == null) {
            if (obra.estado != EstadoObra.ACTIVA) return@mapNotNull null
            val ultima = (obra.ultimaVisita ?: obra.fechaInicio).toLocalDateTime(TimeZone.UTC).date
            return@mapNotNull VisitaAgenda(
                obra, CategoriaAgendaVisita.SIN_PROGRAMAR,
                diasSinVisita = (hoy.toEpochDays().toLong() - ultima.toEpochDays()).coerceAtLeast(0),
            )
        }

        val hora = obra.horaVisita?.takeIf { it in 0 until 24 * 60 }
        val vencio = hora?.let { ahora > LocalDateTime(fecha, LocalTime(it / 60, it % 60)).toInstant(zona) } ?: false
        val categoria = when {
            fecha < hoy || vencio -> CategoriaAgendaVisita.VENCIDAS
            fecha == hoy -> CategoriaAgendaVisita.HOY
            fecha.toEpochDays().toLong() - hoy.toEpochDays() <= 7 -> CategoriaAgendaVisita.ESTA_SEMANA
            else -> CategoriaAgendaVisita.PROXIMAS
        }
        VisitaAgenda(obra, categoria, fecha, hora)
    }.sortedWith(
        compareBy<VisitaAgenda> { it.categoria }
            .thenBy { it.obra.proximaVisita }
            .thenBy { it.obra.horaVisita ?: HORA_POR_DEFECTO }
            .thenByDescending { it.diasSinVisita },
    )
}
