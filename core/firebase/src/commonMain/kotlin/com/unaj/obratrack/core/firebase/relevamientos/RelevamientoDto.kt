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

enum class FirebaseRelevamientoFailure { PermissionDenied, TaskUnavailable, Network, Unexpected }
sealed interface FirebaseRelevamientoResult<out T> {
    data class Success<T>(val value: T) : FirebaseRelevamientoResult<T>
    data class Failure(val error: FirebaseRelevamientoFailure) : FirebaseRelevamientoResult<Nothing>
}

interface FirebaseRelevamientoDataSource {
    fun observarDeObra(userId: String, obraId: String): Flow<FirebaseRelevamientoResult<List<RelevamientoDto>>>
    suspend fun crear(dto: RelevamientoDto): FirebaseRelevamientoResult<RelevamientoDto>
}
