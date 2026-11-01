package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.usecase.RegistrarUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orquesta el formulario de registro; el paso a Home lo dispara la sesión que observa AuthViewModel. */
class RegistroViewModel(
    private val registrar: RegistrarUsuario,
) : ViewModel() {
    private val _state = MutableStateFlow(RegistroUiState())
    val state = _state.asStateFlow()

    fun onEmailChanged(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onRepeatPasswordChanged(value: String) = _state.update { it.copy(repeatPassword = value, error = null) }

    fun submit() {
        val input = _state.value
        if (!input.canSubmit) return
        // Que las contraseñas coincidan es una regla del formulario, no del dominio.
        if (input.password != input.repeatPassword) {
            _state.update { it.copy(error = RegistroError.PasswordsDoNotMatch) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = registrar(input.email, input.password)) {
                // Se limpia el formulario para no conservar la contraseña en memoria.
                is ResultadoRegistro.Exito -> _state.value = RegistroUiState()
                is ResultadoRegistro.Fallo ->
                    _state.update { it.copy(isLoading = false, error = result.error.toRegistroError()) }
            }
        }
    }
}
