package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.Tarea

sealed interface ListaRelevamientosUiState {
    data object Cargando : ListaRelevamientosUiState
    data object Vacia : ListaRelevamientosUiState
    data class Contenido(val items: List<Relevamiento>) : ListaRelevamientosUiState
    data class Error(val motivo: ErrorRelevamientosUi) : ListaRelevamientosUiState
}

enum class ErrorRelevamientosUi {
    NOTA, FECHA, SESION, TAREA, INEXISTENTE, RED, DESCONOCIDO,
    FOTO_SIN_CONEXION, FOTO_MUY_GRANDE, FOTO_FALLO, FOTO_LECTURA,
}

/** Foto que muestra el formulario: ninguna, la que ya estaba guardada o una recién elegida. */
sealed interface FotoFormulario {
    data object Ninguna : FotoFormulario
    data class Existente(val url: String, val vistaUrl: String) : FotoFormulario
    class Nueva(val bytes: ByteArray) : FotoFormulario
}

data class FormularioRelevamientoUiState(
    val id: String? = null,
    val nota: String = "",
    val fechaIso: String = "",
    val tareaId: String? = null,
    val foto: FotoFormulario = FotoFormulario.Ninguna,
    /** Foto guardada al abrir la edición; si se quita, al guardar se elimina. */
    val fotoOriginal: FotoFormulario.Existente? = null,
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
    val seleccionado: Relevamiento? = null,
    val aEliminar: Relevamiento? = null,
    val eliminando: Boolean = false,
    val errorOperacion: ErrorRelevamientosUi? = null,
    val filtro: FiltroRelevamientos = FiltroRelevamientos.Todos,
)
