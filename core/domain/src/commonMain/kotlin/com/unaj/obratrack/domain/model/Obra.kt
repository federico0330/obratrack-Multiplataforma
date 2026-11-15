package com.unaj.obratrack.domain.model

import kotlin.time.Instant

enum class EstadoObra { ACTIVA, PAUSADA, FINALIZADA }

data class Obra(
    val id: String,
    val userId: String,
    val nombre: String,
    /** Opcional: null cuando la obra no tiene cliente cargado. */
    val cliente: String? = null,
    /** Opcional: null cuando la obra no tiene dirección cargada. */
    val direccion: String? = null,
    val publica: Boolean,
    val estado: EstadoObra,
    val fechaInicio: Instant,
    val ultimaVisita: Instant? = null,
    val ubicacion: Ubicacion? = null,
    val proximaVisita: Instant? = null,
    /** Minutos desde medianoche; null cuando la visita no tiene hora asignada. */
    val horaVisita: Int? = null,
)

data class Ubicacion(val latitud: Double, val longitud: Double)

data class ObraDraft(
    val nombre: String,
    val cliente: String? = null,
    val direccion: String? = null,
    val publica: Boolean,
    val fechaInicio: Instant,
)
