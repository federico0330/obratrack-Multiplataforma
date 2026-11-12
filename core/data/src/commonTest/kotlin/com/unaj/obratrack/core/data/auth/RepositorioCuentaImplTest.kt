package com.unaj.obratrack.core.data.auth

import com.unaj.obratrack.core.firebase.auth.AccountFailure
import com.unaj.obratrack.core.firebase.auth.AccountResult
import com.unaj.obratrack.core.firebase.auth.FirebaseAccountDataSource
import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioCuentaImplTest {
    @Test
    fun recuperarExitoPasaElCorreo() = runTest {
        val remote = FuenteCuentaFalsa()

        val result = RepositorioCuentaImpl(remote).recuperarContrasena("a@b.com")

        assertEquals(ResultadoRecuperarContrasena.Exito, result)
        assertEquals("a@b.com", remote.ultimoCorreo)
    }

    @Test
    fun recuperarMapeaLosFallos() = runTest {
        val esperados = mapOf(
            AccountFailure.InvalidEmail to ErrorRecuperarContrasena.EmailInvalido,
            AccountFailure.NoConnection to ErrorRecuperarContrasena.SinConexion,
            AccountFailure.TooManyAttempts to ErrorRecuperarContrasena.DemasiadosIntentos,
            AccountFailure.NoSession to ErrorRecuperarContrasena.Inesperado,
            AccountFailure.Unexpected to ErrorRecuperarContrasena.Inesperado,
        )
        esperados.forEach { (fallo, error) ->
            val remote = FuenteCuentaFalsa(reset = AccountResult.Failure(fallo))

            assertEquals(
                ResultadoRecuperarContrasena.Fallo(error),
                RepositorioCuentaImpl(remote).recuperarContrasena("a@b.com"),
            )
        }
    }

    @Test
    fun actualizarNombreExitoPasaElNombre() = runTest {
        val remote = FuenteCuentaFalsa()

        val result = RepositorioCuentaImpl(remote).actualizarNombre("Ana")

        assertEquals(ResultadoActualizarNombre.Exito, result)
        assertEquals("Ana", remote.ultimoNombre)
    }

    @Test
    fun actualizarNombreMapeaLosFallos() = runTest {
        val esperados = mapOf(
            AccountFailure.NoSession to ErrorActualizarNombre.SinSesion,
            AccountFailure.NoConnection to ErrorActualizarNombre.SinConexion,
            AccountFailure.InvalidEmail to ErrorActualizarNombre.Inesperado,
            AccountFailure.TooManyAttempts to ErrorActualizarNombre.Inesperado,
            AccountFailure.Unexpected to ErrorActualizarNombre.Inesperado,
        )
        esperados.forEach { (fallo, error) ->
            val remote = FuenteCuentaFalsa(update = AccountResult.Failure(fallo))

            assertEquals(
                ResultadoActualizarNombre.Fallo(error),
                RepositorioCuentaImpl(remote).actualizarNombre("Ana"),
            )
        }
    }
}

private class FuenteCuentaFalsa(
    private val reset: AccountResult = AccountResult.Success,
    private val update: AccountResult = AccountResult.Success,
) : FirebaseAccountDataSource {
    var ultimoCorreo: String? = null
    var ultimoNombre: String? = null

    override suspend fun sendPasswordReset(email: String): AccountResult {
        ultimoCorreo = email
        return reset
    }

    override suspend fun updateDisplayName(name: String): AccountResult {
        ultimoNombre = name
        return update
    }
}
