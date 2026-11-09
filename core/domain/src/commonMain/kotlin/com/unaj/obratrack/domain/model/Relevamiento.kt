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
    /** Versión reducida de [fotoUrl] para mostrar en pantalla; igual a [fotoUrl] si no se puede reducir. */
    val fotoVistaUrl: String? = fotoUrl,
)

data class RelevamientoDraft(
    val fecha: Instant,
    val nota: String,
    val tareaId: String? = null,
    val cambioFoto: CambioFoto = CambioFoto.SinCambio,
)

/** Lo que el formulario pide hacer con la foto. */
sealed interface CambioFoto {
    data object SinCambio : CambioFoto
    class Nueva(val bytes: ByteArray) : CambioFoto
    data object Quitar : CambioFoto
}

/** Lo que el repositorio escribe en `fotoUrl`, con la subida ya resuelta. */
sealed interface CambioFotoUrl {
    data object SinCambio : CambioFotoUrl
    data class Nueva(val url: String) : CambioFotoUrl
    data object Quitar : CambioFotoUrl
}
