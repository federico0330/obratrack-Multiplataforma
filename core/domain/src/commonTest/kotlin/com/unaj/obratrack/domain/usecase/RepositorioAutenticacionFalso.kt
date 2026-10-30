package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class RepositorioAutenticacionFalso(
    var resultadoAcceso: ResultadoAcceso? = null,
    var resultadoCerrarSesion: ResultadoCerrarSesion = ResultadoCerrarSesion.Exito,
    sesion: Usuario? = null,
) : RepositorioAutenticacion {
    val sesionActual = MutableStateFlow(sesion)
    override val usuarioActual: Flow<Usuario?> = sesionActual

    var loginCalls = 0
    var lastEmail: String? = null
    var cerrarSesionCalls = 0

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso {
        loginCalls++
        lastEmail = email
        return resultadoAcceso ?: ResultadoAcceso.Exito(Usuario("u1", email, null))
    }

    override suspend fun cerrarSesion(): ResultadoCerrarSesion {
        cerrarSesionCalls++
        return resultadoCerrarSesion
    }
}
