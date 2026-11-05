package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento

class CrearRelevamiento(private val repositorio: RepositorioRelevamientos) {
    suspend operator fun invoke(userId: String, obraId: String, draft: RelevamientoDraft): ResultadoRelevamiento<Relevamiento> {
        val error = when {
            userId.isBlank() || obraId.isBlank() -> ErrorRelevamiento.NO_AUTORIZADA
            draft.fecha.toEpochMilliseconds() <= 0L -> ErrorRelevamiento.FECHA_INVALIDA
            draft.nota.isBlank() -> ErrorRelevamiento.NOTA_VACIA
            draft.tareaId != null && draft.tareaId.isBlank() -> ErrorRelevamiento.TAREA_NO_DISPONIBLE
            else -> null
        }
        if (error != null) return ResultadoRelevamiento.Fallo(error)
        return repositorio.crear(userId, obraId, draft.copy(nota = draft.nota.trim()))
    }
}
