package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.flow.first

sealed interface ResultadoCrearSugeridas {
    data class Exito(val creadas: Int) : ResultadoCrearSugeridas
    data class Fallo(val creadas: Int, val error: ErrorTarea, val omitidas: Int = 0) : ResultadoCrearSugeridas
}

/** Omite títulos existentes y se detiene en el primer error para poder reintentar las tareas restantes. */
class CrearTareasSugeridas(
    private val crearTarea: CrearTarea,
    private val observarTareas: ObservarTareas,
) {
    suspend operator fun invoke(userId: String, obraId: String, drafts: List<TareaDraft>): ResultadoCrearSugeridas {
        if (drafts.isEmpty()) return ResultadoCrearSugeridas.Exito(0)
        val titulos = when (val actuales = observarTareas(userId, obraId).first()) {
            is ResultadoTarea.Fallo -> return ResultadoCrearSugeridas.Fallo(0, actuales.error)
            is ResultadoTarea.Exito -> actuales.dato.map { it.titulo.trim().lowercase() }.toMutableSet()
        }
        var creadas = 0
        var omitidas = 0
        for (draft in drafts) {
            val titulo = draft.titulo.trim().lowercase()
            if (titulo in titulos) {
                omitidas++
                continue
            }
            when (val resultado = crearTarea(userId, obraId, draft)) {
                is ResultadoTarea.Exito -> {
                    titulos += titulo
                    creadas++
                }
                is ResultadoTarea.Fallo -> return ResultadoCrearSugeridas.Fallo(creadas, resultado.error, omitidas)
            }
        }
        return ResultadoCrearSugeridas.Exito(creadas)
    }
}
