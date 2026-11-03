package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.validation.ResultadoValidacionTarea
import com.unaj.obratrack.domain.validation.ValidadorTarea

/** Devuelve el error de validación del borrador, o null si es válido. */
internal fun ValidadorTarea.errorDe(draft: TareaDraft): ErrorTarea? = when (validar(draft)) {
    ResultadoValidacionTarea.Valida -> null
    ResultadoValidacionTarea.TituloVacio -> ErrorTarea.TITULO_VACIO
    ResultadoValidacionTarea.FechasIncompletas -> ErrorTarea.FECHAS_INCOMPLETAS
    ResultadoValidacionTarea.FechaFinAnterior -> ErrorTarea.FECHA_FIN_ANTERIOR
}

internal fun TareaDraft.normalizado(): TareaDraft =
    copy(titulo = titulo.trim(), descripcion = descripcion.trim())
