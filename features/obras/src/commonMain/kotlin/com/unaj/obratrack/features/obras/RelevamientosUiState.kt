package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.Tarea

sealed interface ListaRelevamientosUiState {
    data object Cargando : ListaRelevamientosUiState
    data object Vacia : ListaRelevamientosUiState
    data class Contenido(val items: List<Relevamiento>) : ListaRelevamientosUiState
    data class Error(val motivo: ErrorRelevamientosUi) : ListaRelevamientosUiState
}

enum class ErrorRelevamientosUi { NOTA, FECHA, SESION, TAREA, RED, DESCONOCIDO }

data class FormularioRelevamientoUiState(
    val nota: String = "",
    val fechaIso: String = "",
    val tareaId: String? = null,
    val guardando: Boolean = false,
    val error: ErrorRelevamientosUi? = null,
)

data class OpcionesTareasUiState(
    val items: List<Tarea> = emptyList(),
    val cargando: Boolean = true,
    val error: Boolean = false,
)

data class RelevamientosUiState(
    val lista: ListaRelevamientosUiState = ListaRelevamientosUiState.Cargando,
    val formularioVisible: Boolean = false,
    val formulario: FormularioRelevamientoUiState = FormularioRelevamientoUiState(),
    val tareas: OpcionesTareasUiState = OpcionesTareasUiState(),
)
