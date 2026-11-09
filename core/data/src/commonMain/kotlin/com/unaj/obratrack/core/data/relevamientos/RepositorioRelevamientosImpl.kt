package com.unaj.obratrack.core.data.relevamientos

import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoUpdateFields
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoDataSource
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoFailure
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoResult
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RepositorioRelevamientosImpl(private val remote: FirebaseRelevamientoDataSource) : RepositorioRelevamientos {
    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoRelevamiento<List<Relevamiento>>> =
        remote.observarDeObra(userId, obraId).map { result ->
            val mapped: ResultadoRelevamiento<List<Relevamiento>> = when (result) {
                is FirebaseRelevamientoResult.Success -> ResultadoRelevamiento.Exito(result.value.map { it.toDomain() })
                is FirebaseRelevamientoResult.Failure -> ResultadoRelevamiento.Fallo(result.error.toDomain())
            }
            mapped
        }.catch { error ->
            if (error is CancellationException) throw error
            emit(ResultadoRelevamiento.Fallo(ErrorRelevamiento.DESCONOCIDO))
        }

    override suspend fun crear(
        userId: String, obraId: String, draft: RelevamientoDraft, fotoUrl: String?,
    ): ResultadoRelevamiento<Relevamiento> = try {
        when (val result = remote.crear(draft.toDto(userId, obraId, fotoUrl))) {
            is FirebaseRelevamientoResult.Success -> ResultadoRelevamiento.Exito(result.value.toDomain())
            is FirebaseRelevamientoResult.Failure -> ResultadoRelevamiento.Fallo(result.error.toDomain())
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoRelevamiento.Fallo(ErrorRelevamiento.DESCONOCIDO)
    }

    override suspend fun actualizar(
        userId: String, obraId: String, id: String, draft: RelevamientoDraft, foto: CambioFotoUrl,
    ): ResultadoRelevamiento<Unit> = escribir {
        remote.actualizar(userId, obraId, id,
            RelevamientoUpdateFields(
                draft.fecha.toEpochMilliseconds(), draft.nota, draft.tareaId, foto.toUpdate(), draft.cambioUbicacion.toUpdate(),
            ))
    }

    override suspend fun eliminar(userId: String, obraId: String, id: String): ResultadoRelevamiento<Unit> =
        escribir { remote.eliminar(userId, obraId, id) }

    private suspend inline fun escribir(block: () -> FirebaseRelevamientoResult<Unit>): ResultadoRelevamiento<Unit> = try {
        when (val result = block()) {
            is FirebaseRelevamientoResult.Success -> ResultadoRelevamiento.Exito(Unit)
            is FirebaseRelevamientoResult.Failure -> ResultadoRelevamiento.Fallo(result.error.toDomain())
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoRelevamiento.Fallo(ErrorRelevamiento.DESCONOCIDO)
    }

    private fun FirebaseRelevamientoFailure.toDomain() = when (this) {
        FirebaseRelevamientoFailure.NotFound -> ErrorRelevamiento.NO_ENCONTRADO
        FirebaseRelevamientoFailure.PermissionDenied -> ErrorRelevamiento.NO_AUTORIZADA
        FirebaseRelevamientoFailure.TaskUnavailable -> ErrorRelevamiento.TAREA_NO_DISPONIBLE
        FirebaseRelevamientoFailure.Network -> ErrorRelevamiento.RED
        FirebaseRelevamientoFailure.Unexpected -> ErrorRelevamiento.DESCONOCIDO
    }
}
