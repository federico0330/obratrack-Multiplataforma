package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.model.Usuario

data class AuthUiState(
    val sessionChecked: Boolean = false,
    val user: Usuario? = null,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
