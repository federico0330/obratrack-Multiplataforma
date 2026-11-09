package com.unaj.obratrack.core.firebase.relevamientos

import kotlinx.coroutines.flow.Flow

data class RelevamientoDto(
    val id: String,
    val obraId: String,
    val userId: String,
    val fechaMillis: Long,
    val nota: String,
    val tareaId: String? = null,
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val proximaVisitaMillis: Long? = null,
    val horaVisita: Int? = null,
)

enum class FirebaseRelevamientoFailure { PermissionDenied, TaskUnavailable, NotFound, Network, Unexpected }
sealed interface FirebaseRelevamientoResult<out T> {
    data class Success<T>(val value: T) : FirebaseRelevamientoResult<T>
    data class Failure(val error: FirebaseRelevamientoFailure) : FirebaseRelevamientoResult<Nothing>
}

interface FirebaseRelevamientoDataSource {
    fun observarDeObra(userId: String, obraId: String): Flow<FirebaseRelevamientoResult<List<RelevamientoDto>>>
    suspend fun crear(dto: RelevamientoDto): FirebaseRelevamientoResult<RelevamientoDto>
    suspend fun actualizar(userId: String, obraId: String, id: String, fields: RelevamientoUpdateFields): FirebaseRelevamientoResult<Unit>
    suspend fun eliminar(userId: String, obraId: String, id: String): FirebaseRelevamientoResult<Unit>
}

/** Solo campos editables en este formulario; el resto del documento se conserva. */
data class RelevamientoUpdateFields(
    val fechaMillis: Long,
    val nota: String,
    val tareaId: String?,
    val foto: FotoUpdate = FotoUpdate.SinCambio,
)

/** `SinCambio` no toca `fotoUrl`; `Quitar` elimina el campo. */
sealed interface FotoUpdate {
    data object SinCambio : FotoUpdate
    data class Nueva(val url: String) : FotoUpdate
    data object Quitar : FotoUpdate
}
