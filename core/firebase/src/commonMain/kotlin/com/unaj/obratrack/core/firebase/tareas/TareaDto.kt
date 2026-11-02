package com.unaj.obratrack.core.firebase.tareas

import kotlinx.coroutines.flow.Flow

/**
 * Documento de obras/{obraId}/tareas. Las fechas viajan como string ISO ("2026-10-21") y
 * categoria y estado como el nombre del enum, igual que en los documentos existentes.
 */
data class TareaDto(
    val id: String,
    val obraId: String,
    val userId: String,
    val titulo: String,
    val descripcion: String = "",
    val categoria: String,
    val estado: String,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
)

data class TareaUpdateFields(
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val estado: String,
    val fechaInicio: String?,
    val fechaFin: String?,
)

sealed interface FirebaseTareaFailure {
    data object PermissionDenied : FirebaseTareaFailure
    data object Network : FirebaseTareaFailure
    data object Partial : FirebaseTareaFailure
    data object Unexpected : FirebaseTareaFailure
}

sealed interface FirebaseTareaResult<out T> {
    data class Success<T>(val value: T) : FirebaseTareaResult<T>
    data class Failure(val error: FirebaseTareaFailure) : FirebaseTareaResult<Nothing>
}

interface FirebaseTareaDataSource {
    fun observarDeObra(userId: String, obraId: String): Flow<FirebaseTareaResult<List<TareaDto>>>
    suspend fun crear(dto: TareaDto): FirebaseTareaResult<TareaDto>
    suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        fields: TareaUpdateFields,
    ): FirebaseTareaResult<Unit>
    suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: String,
    ): FirebaseTareaResult<Unit>

    /** Con [eliminarRelevamientos] false, los relevamientos vinculados quedan sin `tareaId`. */
    suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        eliminarRelevamientos: Boolean,
    ): FirebaseTareaResult<Unit>
}
