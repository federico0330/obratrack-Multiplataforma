package com.unaj.obratrack.domain.validation

import com.unaj.obratrack.domain.model.TareaDraft

sealed interface ResultadoValidacionTarea {
    data object Valida : ResultadoValidacionTarea
    data object TituloVacio : ResultadoValidacionTarea
    data object FechasIncompletas : ResultadoValidacionTarea
    data object FechaFinAnterior : ResultadoValidacionTarea
}

/** Regla pura: las fechas van las dos o ninguna, y el fin no puede ser anterior al inicio. */
class ValidadorTarea {
    fun validar(draft: TareaDraft): ResultadoValidacionTarea {
        val inicio = draft.fechaInicio
        val fin = draft.fechaFin
        return when {
            draft.titulo.trim().isEmpty() -> ResultadoValidacionTarea.TituloVacio
            (inicio == null) != (fin == null) -> ResultadoValidacionTarea.FechasIncompletas
            inicio != null && fin != null && fin < inicio -> ResultadoValidacionTarea.FechaFinAnterior
            else -> ResultadoValidacionTarea.Valida
        }
    }
}
