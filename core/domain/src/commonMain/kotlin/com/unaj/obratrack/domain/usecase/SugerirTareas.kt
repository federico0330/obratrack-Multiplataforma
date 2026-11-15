package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.SugeridorDeTareas

class SugerirTareas(private val sugeridor: SugeridorDeTareas) {
    suspend operator fun invoke(tipo: TipoObra): ResultadoSugerir = sugeridor.sugerir(tipo)
}
