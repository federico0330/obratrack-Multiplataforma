package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActualizarNombreUsuarioTest {
    @Test
    fun guardaElNombreRecortado() = runTest {
        val repository = RepositorioCuentaFalso()

        val resultado = ActualizarNombreUsuario(repository)("  Ana Pérez ")

        assertEquals(ResultadoActualizarNombre.Exito, resultado)
        assertEquals("Ana Pérez", repository.lastNombre)
    }

    @Test
    fun nombreEnBlancoNoLlegaAlRepositorio() = runTest {
        val repository = RepositorioCuentaFalso()

        assertEquals(
            ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.NombreInvalido),
            ActualizarNombreUsuario(repository)("   "),
        )
        assertEquals(0, repository.nombreCalls)
    }

    @Test
    fun nombreDemasiadoLargoNoLlegaAlRepositorio() = runTest {
        val repository = RepositorioCuentaFalso()

        assertEquals(
            ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.NombreInvalido),
            ActualizarNombreUsuario(repository)("a".repeat(61)),
        )
        assertEquals(0, repository.nombreCalls)
    }

    @Test
    fun aceptaElLargoMaximo() = runTest {
        val repository = RepositorioCuentaFalso()

        assertEquals(ResultadoActualizarNombre.Exito, ActualizarNombreUsuario(repository)("a".repeat(60)))
    }

    @Test
    fun falloDelRepositorioSePropagaSinLanzar() = runTest {
        val repository = RepositorioCuentaFalso(
            resultadoNombre = ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.SinSesion),
        )

        assertEquals(
            ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.SinSesion),
            ActualizarNombreUsuario(repository)("Ana"),
        )
    }
}
