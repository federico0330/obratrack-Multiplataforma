package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.EstadoObra

sealed interface ListaObrasUiState {
    data object Cargando : ListaObrasUiState
    data object Vacia : ListaObrasUiState
    data class Contenido(val obras: List<Obra>) : ListaObrasUiState
    data class Error(val motivo: ErrorObrasUi) : ListaObrasUiState
}

enum class ErrorObrasUi { SESION, NOMBRE, FECHA, RED, PERMISOS, BAJA_PARCIAL, DESCONOCIDO }

data class FormularioObraUiState(
    val obraId: String? = null,
    val nombre: String = "",
    val cliente: String = "",
    val direccion: String = "",
    val fechaInicio: String = "",
    val publica: Boolean = false,
    val guardando: Boolean = false,
    val error: ErrorObrasUi? = null,
)

data class EdicionVisitaUiState(
    val obra: Obra,
    val guardando: Boolean = false,
    val error: ErrorObrasUi? = null,
)

data class ObrasUiState(
    val lista: ListaObrasUiState = ListaObrasUiState.Cargando,
    val obraSeleccionada: Obra? = null,
    val formularioVisible: Boolean = false,
    val formulario: FormularioObraUiState = FormularioObraUiState(),
    val actualizando: Boolean = false,
    val errorOperacion: ErrorObrasUi? = null,
    val confirmarEliminacion: Boolean = false,
    val eliminando: Boolean = false,
    val edicionVisita: EdicionVisitaUiState? = null,
    /** Aviso de un solo uso: la obra que se acaba de crear. Quien lo atiende llama a `obraCreadaAtendida`. */
    val obraCreadaId: String? = null,
)
