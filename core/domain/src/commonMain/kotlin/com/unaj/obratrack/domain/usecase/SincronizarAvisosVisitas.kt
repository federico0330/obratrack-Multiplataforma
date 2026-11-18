package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.AvisoVisita
import com.unaj.obratrack.domain.repository.ProgramadorAvisos
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** Qué avisos programar y cuáles cancelar. Lo calcula [planificarAvisos] y lo aplica [SincronizarAvisosVisitas]. */
data class PlanAvisos(
    val programar: List<AvisoVisita> = emptyList(),
    val cancelar: List<String> = emptyList(),
) {
    val sinCambios: Boolean get() = programar.isEmpty() && cancelar.isEmpty()
}

/** Hora del aviso cuando la visita no tiene hora: la misma que usa la agenda para ordenar. */
const val HORA_AVISO_POR_DEFECTO = 9 * 60

/**
 * Compara la agenda vigente con los avisos ya programados y arma el plan mínimo para dejarlos
 * iguales: programa las visitas futuras (con la fecha y hora en la zona local del dispositivo) y
 * cancela los avisos cuya visita venció, se reprogramó o desapareció. Las visitas sin fecha no
 * generan avisos. Es puro: se prueba con la agenda real y el puerto queda fuera.
 */
fun planificarAvisos(
    visitas: List<VisitaAgenda>,
    programados: Map<String, Instant>,
    ahora: Instant,
    zona: TimeZone,
): PlanAvisos {
    val esperados = visitas
        .filter { it.fecha != null && it.categoria !in CATEGORIAS_SIN_AVISO }
        .associate { it.obra.id to disparoDe(it, zona) }
        .filterValues { disparo -> disparo > ahora }
    val porProgramar = esperados.filter { (obraId, disparo) -> programados[obraId] != disparo }
        .map { (obraId, disparo) ->
            val visita = visitas.first { it.obra.id == obraId }
            avisoDe(visita, disparo)
        }
    val obsoletos = programados.filter { (obraId, disparo) -> esperados[obraId] != disparo }.keys
    return PlanAvisos(porProgramar, obsoletos.toList())
}

class SincronizarAvisosVisitas(private val programador: ProgramadorAvisos) {
    /** Deja los avisos del dispositivo iguales a la agenda de visitas vigente. */
    suspend operator fun invoke(visitas: List<VisitaAgenda>, ahora: Instant, zona: TimeZone) {
        val plan = planificarAvisos(visitas, programador.programados(), ahora, zona)
        if (plan.sinCambios) return
        plan.cancelar.forEach { programador.cancelar(it) }
        plan.programar.forEach { programador.programar(it) }
    }
}

private val CATEGORIAS_SIN_AVISO = setOf(CategoriaAgendaVisita.VENCIDAS, CategoriaAgendaVisita.SIN_PROGRAMAR)

/** El aviso se dispara a la hora de la visita, o a la hora por defecto si no la tiene. */
private fun minutosDe(visita: VisitaAgenda) =
    visita.horaMinutos?.takeIf { it in 0 until 24 * 60 } ?: HORA_AVISO_POR_DEFECTO

internal fun disparoDe(visita: VisitaAgenda, zona: TimeZone): Instant {
    val minutos = minutosDe(visita)
    return LocalDateTime(visita.fecha!!, LocalTime(minutos / 60, minutos % 60)).toInstant(zona)
}

private fun avisoDe(visita: VisitaAgenda, disparo: Instant) = AvisoVisita(
    obraId = visita.obra.id,
    obraNombre = visita.obra.nombre,
    direccion = visita.obra.direccion,
    fecha = visita.fecha!!,
    horaMinutos = minutosDe(visita),
    disparo = disparo,
)
