package com.unaj.obratrack.core.data.tareas

import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaDataSource
import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaFailure
import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaResult
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RepositorioTareasImpl(
    private val remote: FirebaseTareaDataSource,
) : RepositorioTareas {
    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> =
        remote.observarDeObra(userId, obraId)
            .map { result ->
                val mapped: ResultadoTarea<List<Tarea>> = when (result) {
                    is FirebaseTareaResult.Success -> ResultadoTarea.Exito(result.value.map { it.toDomain() })
                    is FirebaseTareaResult.Failure -> ResultadoTarea.Fallo(result.error.toDomain())
                }
                mapped
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO))
            }

    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> = try {
        when (val result = remote.crear(draft.toDto(userId, obraId))) {
            is FirebaseTareaResult.Success -> ResultadoTarea.Exito(result.value.toDomain())
            is FirebaseTareaResult.Failure -> ResultadoTarea.Fallo(result.error.toDomain())
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO)
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft,
    ): ResultadoTarea<Unit> = try {
        remote.actualizar(userId, obraId, tareaId, draft.toUpdateFields()).toDomainResult()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO)
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: EstadoTarea,
    ): ResultadoTarea<Unit> = try {
        remote.cambiarEstado(userId, obraId, tareaId, estado.name).toDomainResult()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO)
    }

    override suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados,
    ): ResultadoTarea<Unit> = try {
        remote.eliminar(userId, obraId, tareaId, vinculados == RelevamientosVinculados.ELIMINAR)
            .toDomainResult()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO)
    }

    private fun FirebaseTareaResult<Unit>.toDomainResult(): ResultadoTarea<Unit> = when (this) {
        is FirebaseTareaResult.Success -> ResultadoTarea.Exito(Unit)
        is FirebaseTareaResult.Failure -> ResultadoTarea.Fallo(error.toDomain())
    }

    private fun FirebaseTareaFailure.toDomain(): ErrorTarea = when (this) {
        FirebaseTareaFailure.PermissionDenied -> ErrorTarea.NO_AUTORIZADA
        FirebaseTareaFailure.Network -> ErrorTarea.RED
        FirebaseTareaFailure.Partial -> ErrorTarea.FALLO_PARCIAL
        FirebaseTareaFailure.Unexpected -> ErrorTarea.DESCONOCIDO
    }
}
