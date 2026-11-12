package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena
import com.unaj.obratrack.domain.usecase.ActualizarNombreUsuario
import com.unaj.obratrack.domain.usecase.RecuperarContrasena
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gestión de la cuenta: recuperar la contraseña (antes del login) y editar el nombre (en Perfil).
 * El nombre nuevo no se guarda acá: llega por la sesión que observa AuthViewModel.
 */
class CuentaViewModel(
    private val recuperarContrasena: RecuperarContrasena,
    private val actualizarNombre: ActualizarNombreUsuario,
) : ViewModel() {
    private val _state = MutableStateFlow(CuentaUiState())
    val state = _state.asStateFlow()

    private var recuperacionJob: Job? = null
    private var nombreJob: Job? = null

    fun abrirRecuperacion(emailInicial: String) {
        recuperacionJob?.cancel()
        _state.update { it.copy(recuperacion = RecuperacionUiState(email = emailInicial.trim())) }
    }

    fun emailRecuperacionCambiado(value: String) = _state.update {
        it.copy(recuperacion = it.recuperacion?.copy(email = value, error = null))
    }

    fun cerrarRecuperacion() {
        recuperacionJob?.cancel()
        _state.update { it.copy(recuperacion = null) }
    }

    fun enviarRecuperacion() {
        val formulario = _state.value.recuperacion ?: return
        if (!formulario.puedeEnviar || formulario.enviado) return
        recuperacionJob = viewModelScope.launch {
            _state.update { it.copy(recuperacion = it.recuperacion?.copy(enviando = true, error = null)) }
            val resultado = recuperarContrasena(formulario.email)
            _state.update { estado ->
                estado.copy(
                    recuperacion = estado.recuperacion?.let {
                        when (resultado) {
                            ResultadoRecuperarContrasena.Exito -> it.copy(enviando = false, enviado = true)
                            is ResultadoRecuperarContrasena.Fallo ->
                                it.copy(enviando = false, error = resultado.error.toUi())
                        }
                    },
                )
            }
        }
    }

    fun abrirEdicionNombre(nombreActual: String) {
        nombreJob?.cancel()
        _state.update { it.copy(edicionNombre = EdicionNombreUiState(nombre = nombreActual)) }
    }

    fun nombreCambiado(value: String) = _state.update {
        it.copy(edicionNombre = it.edicionNombre?.copy(nombre = value, error = null))
    }

    fun cerrarEdicionNombre() {
        nombreJob?.cancel()
        _state.update { it.copy(edicionNombre = null) }
    }

    fun guardarNombre() {
        val formulario = _state.value.edicionNombre ?: return
        if (!formulario.puedeGuardar) return
        nombreJob = viewModelScope.launch {
            _state.update { it.copy(edicionNombre = it.edicionNombre?.copy(guardando = true, error = null)) }
            when (val resultado = actualizarNombre(formulario.nombre)) {
                ResultadoActualizarNombre.Exito -> _state.update { it.copy(edicionNombre = null) }
                is ResultadoActualizarNombre.Fallo -> _state.update { estado ->
                    estado.copy(
                        edicionNombre = estado.edicionNombre?.copy(guardando = false, error = resultado.error.toUi()),
                    )
                }
            }
        }
    }

    private fun ErrorRecuperarContrasena.toUi() = when (this) {
        ErrorRecuperarContrasena.EmailInvalido -> ErrorRecuperacionUi.EmailInvalido
        ErrorRecuperarContrasena.SinConexion -> ErrorRecuperacionUi.SinConexion
        ErrorRecuperarContrasena.DemasiadosIntentos -> ErrorRecuperacionUi.DemasiadosIntentos
        ErrorRecuperarContrasena.Inesperado -> ErrorRecuperacionUi.Inesperado
    }

    private fun ErrorActualizarNombre.toUi() = when (this) {
        ErrorActualizarNombre.NombreInvalido -> ErrorNombreUi.Invalido
        ErrorActualizarNombre.SinSesion -> ErrorNombreUi.SinSesion
        ErrorActualizarNombre.SinConexion -> ErrorNombreUi.SinConexion
        ErrorActualizarNombre.Inesperado -> ErrorNombreUi.Inesperado
    }
}
