package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.EstadoObra
import kotlinx.coroutines.flow.Flow

enum class ErrorObra {
    NOMBRE_VACIO,
    CLIENTE_VACIO,
    DIRECCION_VACIA,
    NO_AUTORIZADA,
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
}
