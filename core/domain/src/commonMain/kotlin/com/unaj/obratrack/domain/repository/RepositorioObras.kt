package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.EstadoObra
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

enum class ErrorObra {
    NOMBRE_VACIO,
    NO_AUTORIZADA,
    FALLO_PARCIAL,
    RED,
    DESCONOCIDO,
}

sealed interface ResultadoObra<out T> {
    data class Exito<T>(val dato: T) : ResultadoObra<T>
    data class Fallo(val error: ErrorObra) : ResultadoObra<Nothing>
}

/** Puerto de salida del dominio para la colección raíz de obras. */
interface RepositorioObras {
    fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>>
    suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra): ResultadoObra<Obra>
    suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft): ResultadoObra<Unit>
    suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra): ResultadoObra<Unit>
    suspend fun programarVisita(userId: String, obraId: String, fechaUtc: Instant?, horaMinutos: Int?): ResultadoObra<Unit>
    suspend fun eliminar(userId: String, obraId: String): ResultadoObra<Unit>
}
