package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import kotlinx.coroutines.flow.Flow

enum class ErrorRelevamiento {
    NOTA_VACIA, FECHA_INVALIDA, NO_AUTORIZADA, TAREA_NO_DISPONIBLE, NO_ENCONTRADO, RED, DESCONOCIDO,
    FOTO_SIN_CONEXION, FOTO_MUY_GRANDE, FOTO_FALLO,
}

sealed interface ResultadoRelevamiento<out T> {
    data class Exito<T>(val dato: T) : ResultadoRelevamiento<T>
    data class Fallo(val error: ErrorRelevamiento) : ResultadoRelevamiento<Nothing>
}

/** La foto llega ya subida: el repositorio solo escribe su URL y nunca recibe los bytes. */
interface RepositorioRelevamientos {
    fun observarDeObra(userId: String, obraId: String): Flow<ResultadoRelevamiento<List<Relevamiento>>>
    suspend fun crear(userId: String, obraId: String, draft: RelevamientoDraft, fotoUrl: String? = null): ResultadoRelevamiento<Relevamiento>
    suspend fun actualizar(
        userId: String, obraId: String, id: String, draft: RelevamientoDraft, foto: CambioFotoUrl = CambioFotoUrl.SinCambio,
    ): ResultadoRelevamiento<Unit>
    suspend fun eliminar(userId: String, obraId: String, id: String): ResultadoRelevamiento<Unit>
}
