package com.unaj.obratrack.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Un recordatorio local de visita. La app lo programa cuando la agenda tiene una visita futura y la
 * plataforma lo muestra a la hora del [disparo] aunque la app esté cerrada; los textos legibles
 * (fecha, hora y dirección) los forma cada plataforma con su locale al momento de avisar.
 */
data class AvisoVisita(
    val obraId: String,
    val obraNombre: String,
    val direccion: String? = null,
    val fecha: LocalDate,
    val horaMinutos: Int,
    val disparo: Instant,
)
