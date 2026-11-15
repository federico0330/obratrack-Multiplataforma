package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoTarea

sealed interface ResultadoCrearSugeridas {
    data class Exito(val creadas: Int) : ResultadoCrearSugeridas

    /** Se detuvo en el primer error; [creadas] son las que ya quedaron guardadas, en el orden recibido. */
    data class Fallo(val creadas: Int, val error: ErrorTarea) : ResultadoCrearSugeridas
}

/** Crea de a una las tareas elegidas y se detiene ante el primer error, para poder reintentar solo las que faltan. */
class CrearTareasSugeridas(private val crearTarea: CrearTarea) {
    suspend operator fun invoke(userId: String, obraId: String, drafts: List<TareaDraft>): ResultadoCrearSugeridas {
        var creadas = 0
        for (draft in drafts) {
            when (val resultado = crearTarea(userId, obraId, draft)) {
                is ResultadoTarea.Exito -> creadas++
                is ResultadoTarea.Fallo -> return ResultadoCrearSugeridas.Fallo(creadas, resultado.error)
            }
        }
        return ResultadoCrearSugeridas.Exito(creadas)
    }
}
