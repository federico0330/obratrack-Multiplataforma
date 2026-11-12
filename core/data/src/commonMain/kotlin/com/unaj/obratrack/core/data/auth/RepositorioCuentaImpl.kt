package com.unaj.obratrack.core.data.auth

import com.unaj.obratrack.core.firebase.auth.AccountFailure
import com.unaj.obratrack.core.firebase.auth.AccountResult
import com.unaj.obratrack.core.firebase.auth.FirebaseAccountDataSource
import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.RepositorioCuenta
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena

/** Adapta los fallos de infraestructura de la cuenta a los errores del producto. */
class RepositorioCuentaImpl(
    private val remote: FirebaseAccountDataSource,
) : RepositorioCuenta {
    override suspend fun recuperarContrasena(email: String): ResultadoRecuperarContrasena =
        when (val result = remote.sendPasswordReset(email)) {
            AccountResult.Success -> ResultadoRecuperarContrasena.Exito
            is AccountResult.Failure -> ResultadoRecuperarContrasena.Fallo(result.failure.toRecuperacion())
        }

    override suspend fun actualizarNombre(nombre: String): ResultadoActualizarNombre =
        when (val result = remote.updateDisplayName(nombre)) {
            AccountResult.Success -> ResultadoActualizarNombre.Exito
            is AccountResult.Failure -> ResultadoActualizarNombre.Fallo(result.failure.toNombre())
        }
}

private fun AccountFailure.toRecuperacion(): ErrorRecuperarContrasena = when (this) {
    AccountFailure.InvalidEmail -> ErrorRecuperarContrasena.EmailInvalido
    AccountFailure.NoConnection -> ErrorRecuperarContrasena.SinConexion
    AccountFailure.TooManyAttempts -> ErrorRecuperarContrasena.DemasiadosIntentos
    AccountFailure.NoSession, AccountFailure.Unexpected -> ErrorRecuperarContrasena.Inesperado
}

private fun AccountFailure.toNombre(): ErrorActualizarNombre = when (this) {
    AccountFailure.NoSession -> ErrorActualizarNombre.SinSesion
    AccountFailure.NoConnection -> ErrorActualizarNombre.SinConexion
    AccountFailure.InvalidEmail, AccountFailure.TooManyAttempts, AccountFailure.Unexpected ->
        ErrorActualizarNombre.Inesperado
}
