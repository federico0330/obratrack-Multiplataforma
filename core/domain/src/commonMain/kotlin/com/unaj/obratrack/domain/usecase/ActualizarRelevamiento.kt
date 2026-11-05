package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento

class ActualizarRelevamiento(private val repositorio: RepositorioRelevamientos) {
    suspend operator fun invoke(userId: String, obraId: String, id: String, draft: RelevamientoDraft): ResultadoRelevamiento<Unit> {
        val error = validarRelevamiento(userId, obraId, draft)
        if (error != null) return ResultadoRelevamiento.Fallo(error)
        if (id.isBlank()) return ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_ENCONTRADO)
        return repositorio.actualizar(userId, obraId, id, draft.copy(nota = draft.nota.trim()))
    }
}
