package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TipoObra

/** Errores de las sugerencias; el texto sale de recursos, no del ViewModel. */
enum class ErrorSugerenciasUi { SESION, RED, LIMITE, RESPUESTA, DESCONOCIDO }

data class SugerenciasUiState(
    val visible: Boolean = false,
    val tipo: TipoObra? = null,
    val cargando: Boolean = false,
    val sugerencias: List<SugerenciaTarea> = emptyList(),
    val seleccionadas: Set<SugerenciaTarea> = emptySet(),
    val creando: Boolean = false,
    /** Tareas que sí se guardaron antes de un error; ya no figuran en la lista, así que reintentar no las duplica. */
    val creadasAntesDelError: Int = 0,
    val error: ErrorSugerenciasUi? = null,
) {
    val puedeCrear: Boolean get() = seleccionadas.isNotEmpty() && !creando && !cargando
}
