package com.unaj.obratrack.core.data.relevamientos

import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.time.Instant

internal fun RelevamientoDto.toDomain(): Relevamiento {
    val lat = latitud
    val lon = longitud
    return Relevamiento(
        id = id, obraId = obraId, userId = userId,
        fecha = Instant.fromEpochMilliseconds(fechaMillis), nota = nota, tareaId = tareaId, fotoUrl = fotoUrl,
        ubicacion = if (lat != null && lon != null) Ubicacion(lat, lon) else null,
        proximaVisita = proximaVisitaMillis?.let(Instant::fromEpochMilliseconds), horaVisita = horaVisita,
    )
}

internal fun RelevamientoDraft.toDto(userId: String, obraId: String) = RelevamientoDto(
    id = "", obraId = obraId, userId = userId, fechaMillis = fecha.toEpochMilliseconds(), nota = nota, tareaId = tareaId,
)
