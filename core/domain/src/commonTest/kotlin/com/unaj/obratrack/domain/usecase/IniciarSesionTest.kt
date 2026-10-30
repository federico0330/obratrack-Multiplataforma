package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IniciarSesionTest {
    @Test
    fun datosInvalidosNoLlamanAlRepositorio() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = IniciarSesion(repository)("sin-arroba", "clave")

        assertEquals(ResultadoLogin.DatosInvalidos, result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun claveEnBlancoNoLlamaAlRepositorio() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = IniciarSesion(repository)("persona@ejemplo.com", "   ")

        assertEquals(ResultadoLogin.DatosInvalidos, result)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun correoSeRecortaAntesDeAutenticar() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = IniciarSesion(repository)("  persona@ejemplo.com ", "secreto")

        assertEquals(ResultadoLogin.Exito(Usuario("u1", "persona@ejemplo.com", null)), result)
        assertEquals("persona@ejemplo.com", repository.lastEmail)
    }

    @Test
    fun credencialesInvalidasSePropagan() = fallo(ErrorAuth.CredencialesInvalidas)

    @Test
    fun sinConexionSePropaga() = fallo(ErrorAuth.SinConexion)

    @Test
    fun demasiadosIntentosSePropaga() = fallo(ErrorAuth.DemasiadosIntentos)

    @Test
    fun usuarioDeshabilitadoSePropaga() = fallo(ErrorAuth.UsuarioDeshabilitado)

    @Test
    fun errorInesperadoSePropaga() = fallo(ErrorAuth.Inesperado)

    private fun fallo(error: ErrorAuth) = runTest {
        val repository = RepositorioAutenticacionFalso(resultadoAcceso = ResultadoAcceso.Fallo(error))
        val result = IniciarSesion(repository)("persona@ejemplo.com", "secreto")

        assertEquals(ResultadoLogin.Fallo(error), result)
        assertEquals(1, repository.loginCalls)
    }
}
