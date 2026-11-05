package com.unaj.obratrack.domain.repository

/**
 * Puerto separado de [RepositorioAutenticacion] a propósito: sumar el acceso con Google no obliga
 * a tocar los fakes de autenticación que ya existen en otros módulos.
 */
interface RepositorioAccesoGoogle {
    suspend fun iniciarSesionConGoogle(idToken: String): ResultadoAcceso
}

/** Entrega el idToken de Google; cada plataforma lo implementa con su SDK (el dominio no lo conoce). */
interface ProveedorTokenGoogle {
    suspend fun obtenerIdToken(): ResultadoTokenGoogle
}

sealed interface ResultadoTokenGoogle {
    data class Exito(val idToken: String) : ResultadoTokenGoogle
    data object Cancelado : ResultadoTokenGoogle
    data object SinConexion : ResultadoTokenGoogle
    data object Fallo : ResultadoTokenGoogle
}
