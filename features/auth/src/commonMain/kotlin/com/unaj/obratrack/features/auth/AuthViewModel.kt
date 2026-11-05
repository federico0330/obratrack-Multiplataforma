package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.usecase.CerrarSesion
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.IniciarSesionConGoogle
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin
import com.unaj.obratrack.domain.usecase.ResultadoLoginGoogle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orquesta eventos y estado de pantalla; solo habla con casos de uso. */
class AuthViewModel(
    private val login: IniciarSesion,
    private val observarSesion: ObservarSesion,
    private val cerrarSesion: CerrarSesion,
    private val loginConGoogle: IniciarSesionConGoogle,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    private var sessionJob: Job? = null

    init {
        ensureObservingSession()
    }

    /**
     * Si la observación de sesión terminó por una falla del Flow, se reanuda; así un login o un
     * cierre posteriores vuelven a reflejarse en el estado. Si sigue activa, no hace nada.
     */
    private fun ensureObservingSession() {
        if (sessionJob?.isActive == true) return
        sessionJob = viewModelScope.launch {
            observarSesion()
                .catch { error ->
                    if (error is CancellationException) throw error
                    // Sin sesión confirmada: nunca se trata la falla como si hubiera usuario.
                    _state.update {
                        it.copy(sessionChecked = true, user = null, error = AuthError.SessionFailed)
                    }
                }
                .collect { user ->
                    _state.update { it.copy(sessionChecked = true, user = user) }
                }
        }
    }

    fun onEmailChanged(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChanged(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val input = _state.value
        if (!input.canSignIn) return
        ensureObservingSession()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = login(input.email, input.password)
            // El resultado del login es autoritativo aunque la observación de sesión vuelva a fallar.
            _state.update {
                it.copy(
                    isLoading = false,
                    user = (result as? ResultadoLogin.Exito)?.usuario ?: it.user,
                    error = result.toAuthError(),
                )
            }
        }
    }

    fun signInWithGoogle() {
        if (!_state.value.canSignInWithGoogle) return
        ensureObservingSession()
        viewModelScope.launch {
            _state.update { it.copy(isGoogleLoading = true, error = null) }
            val result = loginConGoogle()
            _state.update {
                it.copy(
                    isGoogleLoading = false,
                    user = (result as? ResultadoLoginGoogle.Exito)?.usuario ?: it.user,
                    error = result.toAuthError(),
                )
            }
        }
    }

    fun signOut() {
        ensureObservingSession()
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val error = cerrarSesion().toAuthError()
            if (error != null) _state.update { it.copy(error = error) }
        }
    }
}
