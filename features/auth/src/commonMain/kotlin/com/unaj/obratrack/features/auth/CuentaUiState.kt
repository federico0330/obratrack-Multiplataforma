package com.unaj.obratrack.features.auth

/** Errores del pedido de recuperación; el texto sale de recursos, no del ViewModel. */
enum class ErrorRecuperacionUi { EmailInvalido, SinConexion, DemasiadosIntentos, Inesperado }

/** Errores al editar el nombre; el texto sale de recursos, no del ViewModel. */
enum class ErrorNombreUi { Invalido, SinSesion, SinConexion, Inesperado }

/** Diálogo de recuperación de contraseña; [enviado] reemplaza al formulario por la confirmación. */
data class RecuperacionUiState(
    val email: String = "",
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val error: ErrorRecuperacionUi? = null,
) {
    val puedeEnviar: Boolean get() = email.isNotBlank() && !enviando
}

data class EdicionNombreUiState(
    val nombre: String = "",
    val guardando: Boolean = false,
    val error: ErrorNombreUi? = null,
) {
    val puedeGuardar: Boolean get() = nombre.isNotBlank() && !guardando
}

/** Un diálogo abierto es un estado no nulo; cerrado es null. */
data class CuentaUiState(
    val recuperacion: RecuperacionUiState? = null,
    val edicionNombre: EdicionNombreUiState? = null,
)
