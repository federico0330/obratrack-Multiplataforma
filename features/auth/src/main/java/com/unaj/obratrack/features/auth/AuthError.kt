package com.unaj.obratrack.features.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin

/** Error que la pantalla puede mostrar; el texto sale de recursos, no del ViewModel. */
enum class AuthError {
    InvalidData,
    InvalidCredentials,
    NoConnection,
    TooManyAttempts,
    UserDisabled,
    Unexpected,
    SignOutFailed,
    SessionFailed,
}

fun ErrorAuth.toAuthError(): AuthError = when (this) {
    ErrorAuth.CredencialesInvalidas -> AuthError.InvalidCredentials
    ErrorAuth.SinConexion -> AuthError.NoConnection
    ErrorAuth.DemasiadosIntentos -> AuthError.TooManyAttempts
    ErrorAuth.UsuarioDeshabilitado -> AuthError.UserDisabled
    ErrorAuth.Inesperado -> AuthError.Unexpected
}

/** Null cuando el login fue exitoso. */
fun ResultadoLogin.toAuthError(): AuthError? = when (this) {
    is ResultadoLogin.Exito -> null
    ResultadoLogin.DatosInvalidos -> AuthError.InvalidData
    is ResultadoLogin.Fallo -> error.toAuthError()
}

/** Null cuando se cerró la sesión; cualquier fallo se informa como cierre fallido. */
fun ResultadoCerrarSesion.toAuthError(): AuthError? = when (this) {
    ResultadoCerrarSesion.Exito -> null
    is ResultadoCerrarSesion.Fallo -> AuthError.SignOutFailed
}

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.InvalidData -> R.string.auth_error_invalid_data
    AuthError.InvalidCredentials -> R.string.auth_error_invalid_credentials
    AuthError.NoConnection -> R.string.auth_error_no_connection
    AuthError.TooManyAttempts -> R.string.auth_error_too_many_attempts
    AuthError.UserDisabled -> R.string.auth_error_user_disabled
    AuthError.Unexpected -> R.string.auth_error_unexpected
    AuthError.SignOutFailed -> R.string.auth_error_sign_out_failed
    AuthError.SessionFailed -> R.string.auth_error_session_failed
}

@Composable
fun AuthError.message(): String = stringResource(messageRes())
