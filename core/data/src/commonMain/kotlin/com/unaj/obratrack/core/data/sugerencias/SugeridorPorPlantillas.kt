package com.unaj.obratrack.core.data.sugerencias

import com.unaj.obratrack.domain.model.PlantillasDeTareas
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.SugeridorDeTareas

/** Sugiere las tareas habituales del tipo de obra. Es local: no usa la red, así que nunca falla. */
class SugeridorPorPlantillas : SugeridorDeTareas {
    override suspend fun sugerir(tipo: TipoObra): ResultadoSugerir = ResultadoSugerir.Exito(
        PlantillasDeTareas.para(tipo).map { SugerenciaTarea(TituloSugerido.Plantilla(it), it.categoria) },
    )
}
