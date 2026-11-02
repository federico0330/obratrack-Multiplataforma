package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import kotlinx.coroutines.flow.Flow

enum class ErrorTarea {
    TITULO_VACIO,
    FECHAS_INCOMPLETAS,
    FECHA_FIN_ANTERIOR,
    NO_AUTORIZADA,
    FALLO_PARCIAL,
    RED,
    DESCONOCIDO,
}

sealed interface ResultadoTarea<out T> {
    data class Exito<T>(val dato: T) : ResultadoTarea<T>
    data class Fallo(val error: ErrorTarea) : ResultadoTarea<Nothing>
}

/** Puerto de salida del dominio para la subcolección de tareas de una obra. */
interface RepositorioTareas {
    fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>>
    suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea>
    suspend fun actualizar(userId: String, obraId: String, tareaId: String, draft: TareaDraft): ResultadoTarea<Unit>
    suspend fun cambiarEstado(userId: String, obraId: String, tareaId: String, estado: EstadoTarea): ResultadoTarea<Unit>
    suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados,
    ): ResultadoTarea<Unit>
}
