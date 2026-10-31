package com.unaj.obratrack.features.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.obratrack.domain.repository.ErrorRegistro

/** Error del formulario de registro; el texto sale de recursos, no del ViewModel. */
enum class RegistroError {
    InvalidEmail,
    WeakPassword,
    PasswordsDoNotMatch,
    EmailInUse,
    NoConnection,
    TooManyAttempts,
    Unexpected,
}

fun ErrorRegistro.toRegistroError(): RegistroError = when (this) {
    ErrorRegistro.EmailInvalido -> RegistroError.InvalidEmail
    ErrorRegistro.ContrasenaDebil -> RegistroError.WeakPassword
    ErrorRegistro.EmailYaEnUso -> RegistroError.EmailInUse
    ErrorRegistro.SinConexion -> RegistroError.NoConnection
    ErrorRegistro.DemasiadosIntentos -> RegistroError.TooManyAttempts
    ErrorRegistro.Inesperado -> RegistroError.Unexpected
}

@StringRes
fun RegistroError.messageRes(): Int = when (this) {
    RegistroError.InvalidEmail -> R.string.auth_register_error_invalid_email
    RegistroError.WeakPassword -> R.string.auth_register_error_weak_password
    RegistroError.PasswordsDoNotMatch -> R.string.auth_register_error_passwords_mismatch
    RegistroError.EmailInUse -> R.string.auth_register_error_email_in_use
    RegistroError.NoConnection -> R.string.auth_error_no_connection
    RegistroError.TooManyAttempts -> R.string.auth_error_too_many_attempts
    RegistroError.Unexpected -> R.string.auth_register_error_unexpected
}

@Composable
fun RegistroError.message(): String = stringResource(messageRes())
