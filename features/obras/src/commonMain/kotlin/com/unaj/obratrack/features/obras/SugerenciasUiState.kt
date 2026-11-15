package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TipoObra

/** Errores de las sugerencias; el texto sale de recursos, no del ViewModel. */
enum class ErrorSugerenciasUi { SESION, RED, LIMITE, RESPUESTA, DESCRIPCION, DESCONOCIDO }

data class SugerenciasUiState(
    val visible: Boolean = false,
    val tipo: TipoObra? = null,
    /** Texto libre para sugerir con IA; solo se usa si [iaDisponible]. */
    val descripcion: String = "",
    /** Hay un servicio de IA configurado; sin clave la pantalla solo ofrece las plantillas. */
    val iaDisponible: Boolean = false,
    val cargando: Boolean = false,
    val sugerencias: List<SugerenciaTarea> = emptyList(),
    val seleccionadas: Set<SugerenciaTarea> = emptySet(),
    val creando: Boolean = false,
    /** Tareas que sí se guardaron antes de un error; ya no figuran en la lista, así que reintentar no las duplica. */
    val creadasAntesDelError: Int = 0,
    val error: ErrorSugerenciasUi? = null,
) {
    val puedeCrear: Boolean get() = seleccionadas.isNotEmpty() && !creando && !cargando
    val puedeSugerirConIa: Boolean get() = iaDisponible && descripcion.isNotBlank() && !creando && !cargando
}
