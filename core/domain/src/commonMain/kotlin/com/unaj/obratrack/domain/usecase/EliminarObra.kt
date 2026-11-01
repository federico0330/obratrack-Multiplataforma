package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra

class EliminarObra(private val repositorio: RepositorioObras) {
    suspend operator fun invoke(userId: String, obraId: String): ResultadoObra<Unit> {
        if (userId.isBlank() || obraId.isBlank()) {
            return ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA)
        }
        return repositorio.eliminar(userId, obraId)
    }
}
