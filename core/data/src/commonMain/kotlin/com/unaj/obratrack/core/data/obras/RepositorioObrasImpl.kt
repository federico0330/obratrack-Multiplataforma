package com.unaj.obratrack.core.data.obras

import com.unaj.obratrack.core.firebase.obras.FirebaseObraDataSource
import com.unaj.obratrack.core.firebase.obras.FirebaseObraFailure
import com.unaj.obratrack.core.firebase.obras.FirebaseObraResult
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RepositorioObrasImpl(
    private val remote: FirebaseObraDataSource,
) : RepositorioObras {
    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> =
        remote.observarPropias(userId)
            .map { result ->
                val mapped: ResultadoObra<List<Obra>> = when (result) {
                    is FirebaseObraResult.Success -> ResultadoObra.Exito(result.value.map { it.toDomain() })
                    is FirebaseObraResult.Failure -> ResultadoObra.Fallo(result.error.toDomain())
                }
                mapped
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(ResultadoObra.Fallo(ErrorObra.DESCONOCIDO))
            }

    override suspend fun crear(
        userId: String,
        draft: ObraDraft,
        estado: EstadoObra,
    ): ResultadoObra<Obra> = try {
        when (val result = remote.crear(draft.toDto(userId, estado))) {
            is FirebaseObraResult.Success -> ResultadoObra.Exito(result.value.toDomain())
            is FirebaseObraResult.Failure -> ResultadoObra.Fallo(result.error.toDomain())
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    }

    private fun FirebaseObraFailure.toDomain(): ErrorObra = when (this) {
        FirebaseObraFailure.PermissionDenied -> ErrorObra.NO_AUTORIZADA
        FirebaseObraFailure.Network -> ErrorObra.RED
        FirebaseObraFailure.Unexpected -> ErrorObra.DESCONOCIDO
    }
}
