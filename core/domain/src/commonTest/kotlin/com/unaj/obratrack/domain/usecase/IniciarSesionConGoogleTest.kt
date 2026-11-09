package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.domain.repository.RepositorioAccesoGoogle
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IniciarSesionConGoogleTest {
    private val usuario = Usuario("u1", "persona@gmail.com", "Persona")

    @Test
    fun tokenValidoIniciaSesionConEseToken() = runTest {
        val repositorio = RepositorioAccesoGoogleFalso(ResultadoAcceso.Exito(usuario))
        val result = IniciarSesionConGoogle(ProveedorFalso(ResultadoTokenGoogle.Exito("token")), repositorio)()

        assertEquals(ResultadoLoginGoogle.Exito(usuario), result)
        assertEquals("token", repositorio.ultimoToken)
    }

    @Test
    fun cancelarNoLlamaAlRepositorio() = runTest {
        val repositorio = RepositorioAccesoGoogleFalso()
        val result = IniciarSesionConGoogle(ProveedorFalso(ResultadoTokenGoogle.Cancelado), repositorio)()

        assertEquals(ResultadoLoginGoogle.Cancelado, result)
        assertNull(repositorio.ultimoToken)
    }

    @Test
    fun sinConexionAlPedirTokenEsErrorDeRed() = runTest {
        val result = IniciarSesionConGoogle(
            ProveedorFalso(ResultadoTokenGoogle.SinConexion),
            RepositorioAccesoGoogleFalso(),
        )()

        assertEquals(ResultadoLoginGoogle.Fallo(ErrorAuth.SinConexion), result)
    }

    @Test
    fun falloAlPedirTokenEsInesperado() = runTest {
        val result = IniciarSesionConGoogle(ProveedorFalso(ResultadoTokenGoogle.Fallo), RepositorioAccesoGoogleFalso())()

        assertEquals(ResultadoLoginGoogle.Fallo(ErrorAuth.Inesperado), result)
    }

    @Test
    fun sinConexionDeFirebaseSePropaga() = falloDelRepositorio(ErrorAuth.SinConexion, ErrorAuth.SinConexion)

    @Test
    fun usuarioDeshabilitadoSePropaga() =
        falloDelRepositorio(ErrorAuth.UsuarioDeshabilitado, ErrorAuth.UsuarioDeshabilitado)

    @Test
    fun tokenRechazadoNoSeInformaComoClaveIncorrecta() =
        falloDelRepositorio(ErrorAuth.CredencialesInvalidas, ErrorAuth.Inesperado)

    private fun falloDelRepositorio(delRepositorio: ErrorAuth, esperado: ErrorAuth) = runTest {
        val result = IniciarSesionConGoogle(
            ProveedorFalso(ResultadoTokenGoogle.Exito("token")),
            RepositorioAccesoGoogleFalso(ResultadoAcceso.Fallo(delRepositorio)),
        )()

        assertEquals(ResultadoLoginGoogle.Fallo(esperado), result)
    }
}

private class ProveedorFalso(private val resultado: ResultadoTokenGoogle) : ProveedorTokenGoogle {
    override suspend fun obtenerIdToken() = resultado
}

private class RepositorioAccesoGoogleFalso(
    private val resultado: ResultadoAcceso = ResultadoAcceso.Fallo(ErrorAuth.Inesperado),
) : RepositorioAccesoGoogle {
    var ultimoToken: String? = null

    override suspend fun iniciarSesionConGoogle(idToken: String): ResultadoAcceso {
        ultimoToken = idToken
        return resultado
    }
}
