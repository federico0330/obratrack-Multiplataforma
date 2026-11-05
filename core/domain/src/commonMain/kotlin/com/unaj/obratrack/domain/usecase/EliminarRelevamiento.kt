package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento

class EliminarRelevamiento(private val repositorio: RepositorioRelevamientos) {
    suspend operator fun invoke(userId: String, obraId: String, id: String): ResultadoRelevamiento<Unit> = when {
        userId.isBlank() || obraId.isBlank() -> ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_AUTORIZADA)
        id.isBlank() -> ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_ENCONTRADO)
        else -> repositorio.eliminar(userId, obraId, id)
    }
}
