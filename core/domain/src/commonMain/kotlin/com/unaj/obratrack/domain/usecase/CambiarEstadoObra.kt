package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra

class CambiarEstadoObra(private val repositorio: RepositorioObras) {
    suspend operator fun invoke(userId: String, obraId: String, estado: EstadoObra): ResultadoObra<Unit> {
        if (userId.isBlank() || obraId.isBlank()) {
            return ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA)
        }
        return repositorio.cambiarEstado(userId, obraId, estado)
    }
}
