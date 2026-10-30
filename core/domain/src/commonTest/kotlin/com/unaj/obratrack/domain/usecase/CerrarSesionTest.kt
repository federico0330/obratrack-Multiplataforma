package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CerrarSesionTest {
    @Test
    fun exitoSePropaga() = runTest {
        val repository = RepositorioAutenticacionFalso(resultadoCerrarSesion = ResultadoCerrarSesion.Exito)

        assertEquals(ResultadoCerrarSesion.Exito, CerrarSesion(repository)())
        assertEquals(1, repository.cerrarSesionCalls)
    }

    @Test
    fun falloSePropagaSinLanzar() = runTest {
        val repository = RepositorioAutenticacionFalso(
            resultadoCerrarSesion = ResultadoCerrarSesion.Fallo(ErrorAuth.Inesperado),
        )

        assertEquals(ResultadoCerrarSesion.Fallo(ErrorAuth.Inesperado), CerrarSesion(repository)())
        assertEquals(1, repository.cerrarSesionCalls)
    }
}
