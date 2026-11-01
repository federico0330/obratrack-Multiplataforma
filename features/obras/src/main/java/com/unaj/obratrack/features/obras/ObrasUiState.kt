package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Obra

sealed interface ListaObrasUiState {
    data object Cargando : ListaObrasUiState
    data object Vacia : ListaObrasUiState
    data class Contenido(val obras: List<Obra>) : ListaObrasUiState
    data class Error(val motivo: ErrorObrasUi) : ListaObrasUiState
}

enum class ErrorObrasUi { SESION, NOMBRE, CLIENTE, DIRECCION, FECHA, RED, PERMISOS, DESCONOCIDO }

data class FormularioObraUiState(
    val nombre: String = "",
    val cliente: String = "",
    val direccion: String = "",
    val fechaInicio: String = "",
    val publica: Boolean = false,
    val guardando: Boolean = false,
    val error: ErrorObrasUi? = null,
)

data class ObrasUiState(
    val lista: ListaObrasUiState = ListaObrasUiState.Cargando,
    val formularioVisible: Boolean = false,
    val formulario: FormularioObraUiState = FormularioObraUiState(),
)
