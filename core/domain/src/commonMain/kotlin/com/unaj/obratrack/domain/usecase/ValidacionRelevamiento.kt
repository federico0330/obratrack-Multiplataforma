package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento

internal fun validarRelevamiento(userId: String, obraId: String, draft: RelevamientoDraft): ErrorRelevamiento? = when {
    userId.isBlank() || obraId.isBlank() -> ErrorRelevamiento.NO_AUTORIZADA
    draft.fecha.toEpochMilliseconds() <= 0L -> ErrorRelevamiento.FECHA_INVALIDA
    draft.nota.isBlank() -> ErrorRelevamiento.NOTA_VACIA
    draft.tareaId != null && draft.tareaId.isBlank() -> ErrorRelevamiento.TAREA_NO_DISPONIBLE
    else -> null
}
