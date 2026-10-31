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
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioAutenticacionImplTest {
    @Test
    fun inicioExitosoConvierteElDtoAUsuario() = runTest {
        val remote = FuenteAuthFalsa(signIn = AuthResult.Success(AuthUserDto("u1", "a@b.com", "Ana")))

        val result = RepositorioAutenticacionImpl(remote).iniciarSesion("a@b.com", "x")

        assertEquals(ResultadoAcceso.Exito(Usuario("u1", "a@b.com", "Ana")), result)
    }

    @Test
    fun credencialesInvalidasSeMapean() = falloAcceso(AuthFailure.InvalidCredentials, ErrorAuth.CredencialesInvalidas)

    @Test
    fun sinConexionSeMapea() = falloAcceso(AuthFailure.NoConnection, ErrorAuth.SinConexion)

    @Test
    fun demasiadosIntentosSeMapea() = falloAcceso(AuthFailure.TooManyAttempts, ErrorAuth.DemasiadosIntentos)

    @Test
    fun usuarioDeshabilitadoSeMapea() = falloAcceso(AuthFailure.UserDisabled, ErrorAuth.UsuarioDeshabilitado)

    @Test
    fun inesperadoSeMapea() = falloAcceso(AuthFailure.Unexpected, ErrorAuth.Inesperado)

    @Test
    fun cerrarSesionExitosa() = runTest {
        val remote = FuenteAuthFalsa(signOut = AuthResult.Success(Unit))

        assertEquals(ResultadoCerrarSesion.Exito, RepositorioAutenticacionImpl(remote).cerrarSesion())
    }

    @Test
    fun cerrarSesionFallidaDevuelveFalloSinLanzar() = runTest {
        val remote = FuenteAuthFalsa(signOut = AuthResult.Failure(AuthFailure.Unexpected))

        assertEquals(
            ResultadoCerrarSesion.Fallo(ErrorAuth.Inesperado),
            RepositorioAutenticacionImpl(remote).cerrarSesion(),
        )
    }

    @Test
    fun usuarioActualConvierteElDto() = runTest {
        val remote = FuenteAuthFalsa(usuario = AuthUserDto("u1", "a@b.com", null))

        val usuario = RepositorioAutenticacionImpl(remote).usuarioActual.first()

        assertEquals(Usuario("u1", "a@b.com", null), usuario)
    }

    @Test
    fun registroExitosoConvierteElDtoAUsuario() = runTest {
        val remote = FuenteAuthFalsa(signUp = RegistrationResult.Success(AuthUserDto("u2", "n@b.com", null)))

        val result = RepositorioAutenticacionImpl(remote).registrar("n@b.com", "secreto1")

        assertEquals(ResultadoRegistro.Exito(Usuario("u2", "n@b.com", null)), result)
    }

    @Test
    fun emailEnUsoSeMapea() = falloRegistro(RegistrationFailure.EmailAlreadyInUse, ErrorRegistro.EmailYaEnUso)

    @Test
    fun contrasenaDebilSeMapea() = falloRegistro(RegistrationFailure.WeakPassword, ErrorRegistro.ContrasenaDebil)

    @Test
    fun emailInvalidoSeMapea() = falloRegistro(RegistrationFailure.InvalidEmail, ErrorRegistro.EmailInvalido)

    @Test
    fun registroSinConexionSeMapea() = falloRegistro(RegistrationFailure.NoConnection, ErrorRegistro.SinConexion)

    @Test
    fun registroDemasiadosIntentosSeMapea() =
        falloRegistro(RegistrationFailure.TooManyAttempts, ErrorRegistro.DemasiadosIntentos)

    @Test
    fun registroInesperadoSeMapea() = falloRegistro(RegistrationFailure.Unexpected, ErrorRegistro.Inesperado)

    private fun falloRegistro(failure: RegistrationFailure, esperado: ErrorRegistro) = runTest {
        val remote = FuenteAuthFalsa(signUp = RegistrationResult.Failure(failure))

        val result = RepositorioAutenticacionImpl(remote).registrar("n@b.com", "secreto1")

        assertEquals(ResultadoRegistro.Fallo(esperado), result)
    }

    private fun falloAcceso(failure: AuthFailure, esperado: ErrorAuth) = runTest {
        val remote = FuenteAuthFalsa(signIn = AuthResult.Failure(failure))

        val result = RepositorioAutenticacionImpl(remote).iniciarSesion("a@b.com", "x")

        assertEquals(ResultadoAcceso.Fallo(esperado), result)
    }
}

private class FuenteAuthFalsa(
    private val signIn: AuthResult<AuthUserDto> = AuthResult.Failure(AuthFailure.Unexpected),
    private val signOut: AuthResult<Unit> = AuthResult.Success(Unit),
    private val signUp: RegistrationResult = RegistrationResult.Failure(RegistrationFailure.Unexpected),
    usuario: AuthUserDto? = null,
) : FirebaseAuthDataSource {
    override val currentUser: Flow<AuthUserDto?> = flowOf(usuario)
    override suspend fun signIn(email: String, password: String) = signIn
    override suspend fun signOut() = signOut
    override suspend fun signUp(email: String, password: String) = signUp
}
