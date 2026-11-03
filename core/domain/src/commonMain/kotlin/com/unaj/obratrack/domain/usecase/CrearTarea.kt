package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.validation.ValidadorTarea

class CrearTarea(
    private val repositorio: RepositorioTareas,
    private val validador: ValidadorTarea = ValidadorTarea(),
) {
    suspend operator fun invoke(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> {
        if (userId.isBlank() || obraId.isBlank()) return ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA)
        validador.errorDe(draft)?.let { return ResultadoTarea.Fallo(it) }
        return repositorio.crear(userId, obraId, draft.normalizado())
    }
}
