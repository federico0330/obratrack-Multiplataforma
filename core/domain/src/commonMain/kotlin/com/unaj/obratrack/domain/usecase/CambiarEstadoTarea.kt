package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoTarea

class CambiarEstadoTarea(private val repositorio: RepositorioTareas) {
    suspend operator fun invoke(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: EstadoTarea,
    ): ResultadoTarea<Unit> {
        if (userId.isBlank() || obraId.isBlank() || tareaId.isBlank()) {
            return ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA)
        }
        return repositorio.cambiarEstado(userId, obraId, tareaId, estado)
    }
}
