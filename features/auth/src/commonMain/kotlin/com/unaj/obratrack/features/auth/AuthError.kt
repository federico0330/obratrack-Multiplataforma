package com.unaj.obratrack.features.auth

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.unaj.obratrack.features.auth.generated.resources.Res
import com.unaj.obratrack.features.auth.generated.resources.*
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin
import com.unaj.obratrack.domain.usecase.ResultadoLoginGoogle

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

/** Null cuando entró o cuando el usuario canceló el selector de Google (cancelar no es un error). */
fun ResultadoLoginGoogle.toAuthError(): AuthError? = when (this) {
    is ResultadoLoginGoogle.Exito -> null
    ResultadoLoginGoogle.Cancelado -> null
    is ResultadoLoginGoogle.Fallo -> error.toAuthError()
}

/** Null cuando se cerró la sesión; cualquier fallo se informa como cierre fallido. */
fun ResultadoCerrarSesion.toAuthError(): AuthError? = when (this) {
    ResultadoCerrarSesion.Exito -> null
    is ResultadoCerrarSesion.Fallo -> AuthError.SignOutFailed
}

fun AuthError.messageRes(): StringResource = when (this) {
    AuthError.InvalidData -> Res.string.auth_error_invalid_data
    AuthError.InvalidCredentials -> Res.string.auth_error_invalid_credentials
    AuthError.NoConnection -> Res.string.auth_error_no_connection
    AuthError.TooManyAttempts -> Res.string.auth_error_too_many_attempts
    AuthError.UserDisabled -> Res.string.auth_error_user_disabled
    AuthError.Unexpected -> Res.string.auth_error_unexpected
    AuthError.SignOutFailed -> Res.string.auth_error_sign_out_failed
    AuthError.SessionFailed -> Res.string.auth_error_session_failed
}

@Composable
fun AuthError.message(): String = stringResource(messageRes())
