package com.unaj.obratrack.core.firebase.obras

data class ObraDto(
    val id: String,
    val userId: String,
    val nombre: String,
    val cliente: String? = null,
    val direccion: String? = null,
    val publica: Boolean,
    val estado: String,
    val fechaInicioMillis: Long,
    val ultimaVisitaMillis: Long? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val proximaVisitaMillis: Long? = null,
    val horaVisita: Int? = null,
)

sealed interface FirebaseObraFailure {
    data object PermissionDenied : FirebaseObraFailure
    data object Network : FirebaseObraFailure
    data object Partial : FirebaseObraFailure
    data object Unexpected : FirebaseObraFailure
}

sealed interface FirebaseObraResult<out T> {
    data class Success<T>(val value: T) : FirebaseObraResult<T>
    data class Failure(val error: FirebaseObraFailure) : FirebaseObraResult<Nothing>
}

interface FirebaseObraDataSource {
    fun observarPropias(userId: String): kotlinx.coroutines.flow.Flow<FirebaseObraResult<List<ObraDto>>>
    suspend fun crear(dto: ObraDto): FirebaseObraResult<ObraDto>
    suspend fun actualizar(userId: String, obraId: String, fields: ObraUpdateFields): FirebaseObraResult<Unit>
    suspend fun cambiarEstado(userId: String, obraId: String, estado: String): FirebaseObraResult<Unit>
    suspend fun programarVisita(userId: String, obraId: String, fechaMillis: Long?, horaMinutos: Int?): FirebaseObraResult<Unit>
    suspend fun eliminar(userId: String, obraId: String): FirebaseObraResult<Unit>
}

data class ObraUpdateFields(
    val nombre: String,
    val cliente: String? = null,
    val direccion: String? = null,
    val publica: Boolean,
    val fechaInicioMillis: Long,
)
