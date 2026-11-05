package com.unaj.obratrack.domain.model

import kotlin.time.Instant

data class Relevamiento(
    val id: String,
    val obraId: String,
    val userId: String,
    val fecha: Instant,
    val nota: String,
    val tareaId: String? = null,
    val fotoUrl: String? = null,
    val ubicacion: Ubicacion? = null,
    val proximaVisita: Instant? = null,
    val horaVisita: Int? = null,
)

data class RelevamientoDraft(val fecha: Instant, val nota: String, val tareaId: String? = null)
