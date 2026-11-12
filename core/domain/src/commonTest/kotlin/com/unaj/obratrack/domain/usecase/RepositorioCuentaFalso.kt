package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.RepositorioCuenta
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena

internal class RepositorioCuentaFalso(
    var resultadoRecuperar: ResultadoRecuperarContrasena = ResultadoRecuperarContrasena.Exito,
    var resultadoNombre: ResultadoActualizarNombre = ResultadoActualizarNombre.Exito,
) : RepositorioCuenta {
    var recuperarCalls = 0
    var lastEmail: String? = null
    var nombreCalls = 0
    var lastNombre: String? = null

    override suspend fun recuperarContrasena(email: String): ResultadoRecuperarContrasena {
        recuperarCalls++
        lastEmail = email
        return resultadoRecuperar
    }

    override suspend fun actualizarNombre(nombre: String): ResultadoActualizarNombre {
        nombreCalls++
        lastNombre = nombre
        return resultadoNombre
    }
}
