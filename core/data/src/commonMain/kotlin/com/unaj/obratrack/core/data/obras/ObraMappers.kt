package com.unaj.obratrack.core.data.obras

import com.unaj.obratrack.core.firebase.obras.ObraDto
import com.unaj.obratrack.core.firebase.obras.ObraUpdateFields
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.time.Instant

internal fun ObraDto.toDomain(): Obra = Obra(
    id = id,
    userId = userId,
    nombre = nombre,
    cliente = cliente,
    direccion = direccion,
    publica = publica,
    estado = when (estado) {
        "activa" -> EstadoObra.ACTIVA
        "pausada" -> EstadoObra.PAUSADA
        "finalizada" -> EstadoObra.FINALIZADA
        else -> error("Estado de obra desconocido")
    },
    fechaInicio = Instant.fromEpochMilliseconds(fechaInicioMillis),
    ultimaVisita = ultimaVisitaMillis?.let(Instant::fromEpochMilliseconds),
    ubicacion = latitud?.let { latitude ->
        longitud?.let { longitude -> Ubicacion(latitude, longitude) }
    },
    proximaVisita = proximaVisitaMillis?.let(Instant::fromEpochMilliseconds),
    horaVisita = horaVisita,
)

internal fun ObraDraft.toDto(userId: String, estado: EstadoObra): ObraDto = ObraDto(
    id = "",
    userId = userId,
    nombre = nombre,
    cliente = cliente,
    direccion = direccion,
    publica = publica,
    estado = when (estado) {
        EstadoObra.ACTIVA -> "activa"
        EstadoObra.PAUSADA -> "pausada"
        EstadoObra.FINALIZADA -> "finalizada"
    },
    fechaInicioMillis = fechaInicio.toEpochMilliseconds(),
)

internal fun ObraDraft.toUpdateFields(): ObraUpdateFields = ObraUpdateFields(
    nombre = nombre,
    cliente = cliente,
    direccion = direccion,
    publica = publica,
    fechaInicioMillis = fechaInicio.toEpochMilliseconds(),
)

internal fun EstadoObra.toFirestoreValue(): String = when (this) {
    EstadoObra.ACTIVA -> "activa"
    EstadoObra.PAUSADA -> "pausada"
    EstadoObra.FINALIZADA -> "finalizada"
}
