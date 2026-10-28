package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orquesta eventos y estado de pantalla; no contiene widgets ni detalles de Firebase. */
class AuthViewModel(
    private val login: IniciarSesion,
    private val repository: RepositorioAutenticacion,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.usuarioActual.collect { user ->
                _state.update { it.copy(sessionChecked = true, user = user) }
            }
        }
    }

    fun onEmailChanged(value: String) = _state.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value, errorMessage = null) }

    fun signIn() {
        val input = _state.value
        if (input.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = login(input.email, input.password)
                val message = when (result) {
                    is ResultadoLogin.Exito -> null
                    ResultadoLogin.DatosInvalidos -> "Revisá el correo y la contraseña ingresados."
                    ResultadoLogin.CredencialesInvalidas -> "El correo o la contraseña no son correctos."
                    ResultadoLogin.ErrorDeRed -> "No hay conexión. Revisá internet e intentá de nuevo."
                    ResultadoLogin.ErrorInesperado -> "No pudimos iniciar sesión. Intentá nuevamente."
                }
                _state.update { it.copy(isLoading = false, errorMessage = message) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "No pudimos iniciar sesión. Intentá nuevamente.") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                repository.cerrarSesion()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(errorMessage = "No pudimos cerrar la sesión. Intentá nuevamente.") }
            }
        }
    }
}
