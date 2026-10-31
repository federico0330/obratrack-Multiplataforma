package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class RepositorioAutenticacionFalso(
    var resultadoAcceso: ResultadoAcceso? = null,
    var resultadoCerrarSesion: ResultadoCerrarSesion = ResultadoCerrarSesion.Exito,
    var resultadoRegistro: ResultadoRegistro? = null,
    sesion: Usuario? = null,
) : RepositorioAutenticacion {
    val sesionActual = MutableStateFlow(sesion)
    override val usuarioActual: Flow<Usuario?> = sesionActual

    var loginCalls = 0
    var lastEmail: String? = null
    var cerrarSesionCalls = 0
    var registrarCalls = 0
    var lastRegistroEmail: String? = null

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso {
        loginCalls++
        lastEmail = email
        return resultadoAcceso ?: ResultadoAcceso.Exito(Usuario("u1", email, null))
    }

    override suspend fun cerrarSesion(): ResultadoCerrarSesion {
        cerrarSesionCalls++
        return resultadoCerrarSesion
    }

    override suspend fun registrar(email: String, contrasena: String): ResultadoRegistro {
        registrarCalls++
        lastRegistroEmail = email
        return resultadoRegistro ?: ResultadoRegistro.Exito(Usuario("u1", email, null))
    }
}
