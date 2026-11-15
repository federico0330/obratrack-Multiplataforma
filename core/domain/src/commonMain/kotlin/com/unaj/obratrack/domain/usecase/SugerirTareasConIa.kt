package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.SugeridorConIA
import com.unaj.obratrack.domain.validation.ValidadorDescripcionObra

/** Pide tareas a un servicio externo. Valida y recorta antes de enviar: lo que se manda es solo este texto. */
class SugerirTareasConIa(
    private val sugeridor: SugeridorConIA,
    private val validador: ValidadorDescripcionObra = ValidadorDescripcionObra(),
) {
    suspend operator fun invoke(descripcion: String): ResultadoSugerir {
        if (!validador.descripcionValida(descripcion)) return ResultadoSugerir.Fallo(ErrorSugerir.DESCRIPCION_INVALIDA)
        return sugeridor.sugerir(validador.normalizar(descripcion))
    }
}
