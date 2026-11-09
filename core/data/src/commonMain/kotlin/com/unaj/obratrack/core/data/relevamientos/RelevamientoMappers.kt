package com.unaj.obratrack.core.data.relevamientos

import com.unaj.obratrack.core.data.fotos.urlMiniaturaCloudinary
import com.unaj.obratrack.core.data.fotos.urlReducidaCloudinary
import com.unaj.obratrack.core.firebase.relevamientos.FotoUpdate
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import com.unaj.obratrack.core.firebase.relevamientos.UbicacionUpdate
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.CambioUbicacion
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
        fotoVistaUrl = fotoUrl?.let(::urlReducidaCloudinary),
        fotoMiniaturaUrl = fotoUrl?.let(::urlMiniaturaCloudinary),
    )
}

internal fun RelevamientoDraft.toDto(userId: String, obraId: String, fotoUrl: String?): RelevamientoDto {
    // Al crear no hay ubicación previa: quitarla equivale a no tenerla.
    val ubicacion = (cambioUbicacion as? CambioUbicacion.Nueva)?.ubicacion
    return RelevamientoDto(
        id = "", obraId = obraId, userId = userId, fechaMillis = fecha.toEpochMilliseconds(), nota = nota, tareaId = tareaId,
        fotoUrl = fotoUrl, latitud = ubicacion?.latitud, longitud = ubicacion?.longitud,
    )
}

internal fun CambioUbicacion.toUpdate(): UbicacionUpdate = when (this) {
    CambioUbicacion.SinCambio -> UbicacionUpdate.SinCambio
    is CambioUbicacion.Nueva -> UbicacionUpdate.Nueva(ubicacion.latitud, ubicacion.longitud)
    CambioUbicacion.Quitar -> UbicacionUpdate.Quitar
}

internal fun CambioFotoUrl.toUpdate(): FotoUpdate = when (this) {
    CambioFotoUrl.SinCambio -> FotoUpdate.SinCambio
    is CambioFotoUrl.Nueva -> FotoUpdate.Nueva(url)
    CambioFotoUrl.Quitar -> FotoUpdate.Quitar
}
