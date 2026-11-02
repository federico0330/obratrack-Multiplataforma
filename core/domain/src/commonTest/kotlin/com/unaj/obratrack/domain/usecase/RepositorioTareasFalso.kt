package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class RepositorioTareasFalso : RepositorioTareas {
    var llamadas = 0
    var userId: String? = null
    var obraId: String? = null
    var tareaId: String? = null
    var draft: TareaDraft? = null
    var estado: EstadoTarea? = null
    var vinculados: RelevamientosVinculados? = null
    var tareas: ResultadoTarea<List<Tarea>> = ResultadoTarea.Exito(emptyList())
    var resultadoUnit: ResultadoTarea<Unit> = ResultadoTarea.Exito(Unit)

    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> = flowOf(tareas)

    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> {
        registrar(userId, obraId, null, draft)
        return ResultadoTarea.Exito(
            Tarea(
                id = "tarea-1",
                obraId = obraId,
                userId = userId,
                titulo = draft.titulo,
                descripcion = draft.descripcion,
                categoria = draft.categoria,
                fechaInicio = draft.fechaInicio,
                fechaFin = draft.fechaFin,
                estado = draft.estado,
            ),
        )
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft,
    ): ResultadoTarea<Unit> {
        registrar(userId, obraId, tareaId, draft)
        return resultadoUnit
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: EstadoTarea,
    ): ResultadoTarea<Unit> {
        registrar(userId, obraId, tareaId, null)
        this.estado = estado
        return resultadoUnit
    }

    override suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados,
    ): ResultadoTarea<Unit> {
        registrar(userId, obraId, tareaId, null)
        this.vinculados = vinculados
        return resultadoUnit
    }

    private fun registrar(userId: String, obraId: String, tareaId: String?, draft: TareaDraft?) {
        llamadas++
        this.userId = userId
        this.obraId = obraId
        this.tareaId = tareaId
        this.draft = draft
    }
}
