package com.unaj.obratrack.features.auth

data class RegistroUiState(
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val isLoading: Boolean = false,
    val error: RegistroError? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && repeatPassword.isNotBlank() && !isLoading
}
