package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecuperarContrasenaTest {
    @Test
    fun normalizaElCorreoYPideElEnvio() = runTest {
        val repository = RepositorioCuentaFalso()

        val resultado = RecuperarContrasena(repository)("  ana@mail.com ")

        assertEquals(ResultadoRecuperarContrasena.Exito, resultado)
        assertEquals("ana@mail.com", repository.lastEmail)
    }

    @Test
    fun correoInvalidoNoLlegaAlRepositorio() = runTest {
        val repository = RepositorioCuentaFalso()

        val resultado = RecuperarContrasena(repository)("ana@")

        assertEquals(ResultadoRecuperarContrasena.Fallo(ErrorRecuperarContrasena.EmailInvalido), resultado)
        assertEquals(0, repository.recuperarCalls)
    }

    @Test
    fun falloDelRepositorioSePropagaSinLanzar() = runTest {
        val repository = RepositorioCuentaFalso(
            resultadoRecuperar = ResultadoRecuperarContrasena.Fallo(ErrorRecuperarContrasena.SinConexion),
        )

        val resultado = RecuperarContrasena(repository)("ana@mail.com")

        assertEquals(ResultadoRecuperarContrasena.Fallo(ErrorRecuperarContrasena.SinConexion), resultado)
    }
}
