package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
    fun correoSeRecortaAntesDeAutenticar() = runTest {
        val repository = RepositorioAutenticacionFalso()
        val result = IniciarSesion(repository)("  persona@ejemplo.com ", "secreto")

        assertEquals(ResultadoLogin.Exito(Usuario("u1", "persona@ejemplo.com", null)), result)
        assertEquals("persona@ejemplo.com", repository.lastEmail)
    }
}

private class RepositorioAutenticacionFalso : RepositorioAutenticacion {
    override val usuarioActual: Flow<Usuario?> = emptyFlow()
    var loginCalls = 0
    var lastEmail: String? = null

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso {
        loginCalls++
        lastEmail = email
        return ResultadoAcceso.Exito(Usuario("u1", email, null))
    }

    override suspend fun cerrarSesion() = Unit
}
