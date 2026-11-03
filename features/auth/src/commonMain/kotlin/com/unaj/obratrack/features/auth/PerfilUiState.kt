package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.model.ResumenObras

enum class ErrorResumenUi { SESION, RED, PERMISOS, DESCONOCIDO }

/** Estado del bloque de resumen de obras; los datos de la cuenta no dependen de él. */
sealed interface ResumenPerfilUiState {
    data object Cargando : ResumenPerfilUiState

    /** Una lista vacía es contenido (todo en cero), no un error. */
    data class Contenido(val resumen: ResumenObras) : ResumenPerfilUiState

    data class Error(val motivo: ErrorResumenUi) : ResumenPerfilUiState
}
