package com.unaj.obratrack.core.data.auth

import com.unaj.obratrack.core.firebase.auth.AuthFailure
import com.unaj.obratrack.core.firebase.auth.AuthResult
import com.unaj.obratrack.core.firebase.auth.AuthUserDto
import com.unaj.obratrack.core.firebase.auth.FirebaseAuthDataSource
import com.unaj.obratrack.core.firebase.auth.RegistrationFailure
import com.unaj.obratrack.core.firebase.auth.RegistrationResult
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ErrorRegistro
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import kotlinx.coroutines.flow.map

/** Adapta los DTO y fallos de infraestructura a los modelos que consume la aplicación. */
class RepositorioAutenticacionImpl(
    private val remote: FirebaseAuthDataSource,
) : RepositorioAutenticacion {
    override val usuarioActual = remote.currentUser.map { it?.toDomain() }

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso =
        when (val result = remote.signIn(email, clave)) {
            is AuthResult.Success -> ResultadoAcceso.Exito(result.value.toDomain())
            is AuthResult.Failure -> ResultadoAcceso.Fallo(result.failure.toDomain())
        }

    override suspend fun cerrarSesion(): ResultadoCerrarSesion =
        when (val result = remote.signOut()) {
            is AuthResult.Success -> ResultadoCerrarSesion.Exito
            is AuthResult.Failure -> ResultadoCerrarSesion.Fallo(result.failure.toDomain())
        }

    override suspend fun registrar(email: String, contrasena: String): ResultadoRegistro =
        when (val result = remote.signUp(email, contrasena)) {
            is RegistrationResult.Success -> ResultadoRegistro.Exito(result.user.toDomain())
            is RegistrationResult.Failure -> ResultadoRegistro.Fallo(result.failure.toDomain())
        }

    private fun AuthUserDto.toDomain() = Usuario(id = id, email = email, nombre = displayName)
}

internal fun AuthFailure.toDomain(): ErrorAuth = when (this) {
    AuthFailure.InvalidCredentials -> ErrorAuth.CredencialesInvalidas
    AuthFailure.NoConnection -> ErrorAuth.SinConexion
    AuthFailure.TooManyAttempts -> ErrorAuth.DemasiadosIntentos
    AuthFailure.UserDisabled -> ErrorAuth.UsuarioDeshabilitado
    AuthFailure.Unexpected -> ErrorAuth.Inesperado
}

internal fun RegistrationFailure.toDomain(): ErrorRegistro = when (this) {
    RegistrationFailure.EmailAlreadyInUse -> ErrorRegistro.EmailYaEnUso
    RegistrationFailure.WeakPassword -> ErrorRegistro.ContrasenaDebil
    RegistrationFailure.InvalidEmail -> ErrorRegistro.EmailInvalido
    RegistrationFailure.NoConnection -> ErrorRegistro.SinConexion
    RegistrationFailure.TooManyAttempts -> ErrorRegistro.DemasiadosIntentos
    RegistrationFailure.Unexpected -> ErrorRegistro.Inesperado
}
