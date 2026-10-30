package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthErrorTest {
    @Test
    fun credencialesInvalidas() = assertEquals(AuthError.InvalidCredentials, ErrorAuth.CredencialesInvalidas.toAuthError())

    @Test
    fun sinConexion() = assertEquals(AuthError.NoConnection, ErrorAuth.SinConexion.toAuthError())

    @Test
    fun demasiadosIntentos() = assertEquals(AuthError.TooManyAttempts, ErrorAuth.DemasiadosIntentos.toAuthError())

    @Test
    fun usuarioDeshabilitado() = assertEquals(AuthError.UserDisabled, ErrorAuth.UsuarioDeshabilitado.toAuthError())

    @Test
    fun inesperado() = assertEquals(AuthError.Unexpected, ErrorAuth.Inesperado.toAuthError())

    @Test
    fun loginExitosoNoTieneError() =
        assertNull(ResultadoLogin.Exito(Usuario("u1", "a@b.com", null)).toAuthError())

    @Test
    fun loginConDatosInvalidos() = assertEquals(AuthError.InvalidData, ResultadoLogin.DatosInvalidos.toAuthError())

    @Test
    fun loginFalloUsaElMapeoDeErrorAuth() =
        assertEquals(AuthError.NoConnection, ResultadoLogin.Fallo(ErrorAuth.SinConexion).toAuthError())

    @Test
    fun cierreExitosoNoTieneError() = assertNull(ResultadoCerrarSesion.Exito.toAuthError())

    @Test
    fun cierreFallidoEsSignOutFailed() =
        assertEquals(AuthError.SignOutFailed, ResultadoCerrarSesion.Fallo(ErrorAuth.Inesperado).toAuthError())

    @Test
    fun puedeIngresarSoloConCamposCompletosYSinCargar() {
        assertEquals(false, AuthUiState().canSignIn)
        assertEquals(false, AuthUiState(email = "a@b.com").canSignIn)
        assertEquals(false, AuthUiState(email = "a@b.com", password = "  ").canSignIn)
        assertEquals(true, AuthUiState(email = "a@b.com", password = "x").canSignIn)
        assertEquals(false, AuthUiState(email = "a@b.com", password = "x", isLoading = true).canSignIn)
    }
}
