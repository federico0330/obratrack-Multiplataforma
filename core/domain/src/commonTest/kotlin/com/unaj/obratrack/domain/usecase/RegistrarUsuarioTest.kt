package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorRegistro
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistrarUsuarioTest {
    @Test
    fun emailSinArrobaNoLlamaAlRepositorio() = invalido("sin-arroba", "secreto1", ErrorRegistro.EmailInvalido)

    @Test
    fun emailSinDominioConPuntoNoLlamaAlRepositorio() =
        invalido("persona@ejemplo", "secreto1", ErrorRegistro.EmailInvalido)

    @Test
    fun emailSinParteLocalNoLlamaAlRepositorio() = invalido("@ejemplo.com", "secreto1", ErrorRegistro.EmailInvalido)

    @Test
    fun emailConEspaciosInternosNoLlamaAlRepositorio() =
        invalido("per sona@ejemplo.com", "secreto1", ErrorRegistro.EmailInvalido)

    @Test
    fun emailVacioNoLlamaAlRepositorio() = invalido("   ", "secreto1", ErrorRegistro.EmailInvalido)

    @Test
    fun contrasenaCortaNoLlamaAlRepositorio() = invalido("persona@ejemplo.com", "12345", ErrorRegistro.ContrasenaDebil)

    @Test
    fun contrasenaEnBlancoNoLlamaAlRepositorio() =
        invalido("persona@ejemplo.com", "       ", ErrorRegistro.ContrasenaDebil)

    @Test
    fun contrasenaDeSeisCaracteresEsValida() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = RegistrarUsuario(repository)("persona@ejemplo.com", "123456")

        assertEquals(ResultadoRegistro.Exito(Usuario("u1", "persona@ejemplo.com", null)), result)
    }

    @Test
    fun correoSeRecortaAntesDeRegistrar() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = RegistrarUsuario(repository)("  persona@ejemplo.com ", "secreto1")

        assertEquals(ResultadoRegistro.Exito(Usuario("u1", "persona@ejemplo.com", null)), result)
        assertEquals("persona@ejemplo.com", repository.lastRegistroEmail)
        assertEquals(1, repository.registrarCalls)
    }

    @Test
    fun emailYaEnUsoSePropaga() = fallo(ErrorRegistro.EmailYaEnUso)

    @Test
    fun contrasenaDebilDelServidorSePropaga() = fallo(ErrorRegistro.ContrasenaDebil)

    @Test
    fun emailInvalidoDelServidorSePropaga() = fallo(ErrorRegistro.EmailInvalido)

    @Test
    fun sinConexionSePropaga() = fallo(ErrorRegistro.SinConexion)

    @Test
    fun demasiadosIntentosSePropaga() = fallo(ErrorRegistro.DemasiadosIntentos)

    @Test
    fun errorInesperadoSePropaga() = fallo(ErrorRegistro.Inesperado)

    private fun invalido(email: String, contrasena: String, esperado: ErrorRegistro) = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = RegistrarUsuario(repository)(email, contrasena)

        assertEquals(ResultadoRegistro.Fallo(esperado), result)
        assertEquals(0, repository.registrarCalls)
    }

    private fun fallo(error: ErrorRegistro) = runTest {
        val repository = RepositorioAutenticacionFalso(resultadoRegistro = ResultadoRegistro.Fallo(error))
        val result = RegistrarUsuario(repository)("persona@ejemplo.com", "secreto1")

        assertEquals(ResultadoRegistro.Fallo(error), result)
        assertEquals(1, repository.registrarCalls)
    }
}
