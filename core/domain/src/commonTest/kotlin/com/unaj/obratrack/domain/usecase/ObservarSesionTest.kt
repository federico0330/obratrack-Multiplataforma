package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObservarSesionTest {
    @Test
    fun sinSesionEmiteNull() = runTest {
        val repository = RepositorioAutenticacionFalso(sesion = null)

        assertNull(ObservarSesion(repository)().first())
    }

    @Test
    fun conSesionEmiteElUsuario() = runTest {
        val usuario = Usuario("u1", "persona@ejemplo.com", null)
        val repository = RepositorioAutenticacionFalso(sesion = usuario)

        assertEquals(usuario, ObservarSesion(repository)().first())
    }

    @Test
    fun reflejaLosCambiosDeSesion() = runTest {
        val repository = RepositorioAutenticacionFalso(sesion = null)
        val observar = ObservarSesion(repository)
        val usuario = Usuario("u1", "persona@ejemplo.com", null)

        repository.sesionActual.value = usuario
        assertEquals(usuario, observar().first())

        repository.sesionActual.value = null
        assertNull(observar().first())
    }
}
