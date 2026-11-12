package com.unaj.obratrack.domain.repository

/**
 * Puerto separado de [RepositorioAutenticacion] a propósito: sumar la gestión de cuenta no obliga
 * a tocar los fakes de autenticación que ya existen en otros módulos.
 */
interface RepositorioCuenta {
    suspend fun recuperarContrasena(email: String): ResultadoRecuperarContrasena
    suspend fun actualizarNombre(nombre: String): ResultadoActualizarNombre
}

sealed interface ErrorRecuperarContrasena {
    data object EmailInvalido : ErrorRecuperarContrasena
    data object SinConexion : ErrorRecuperarContrasena
    data object DemasiadosIntentos : ErrorRecuperarContrasena
    data object Inesperado : ErrorRecuperarContrasena
}

sealed interface ResultadoRecuperarContrasena {
    data object Exito : ResultadoRecuperarContrasena
    data class Fallo(val error: ErrorRecuperarContrasena) : ResultadoRecuperarContrasena
}

sealed interface ErrorActualizarNombre {
    data object NombreInvalido : ErrorActualizarNombre
    data object SinSesion : ErrorActualizarNombre
    data object SinConexion : ErrorActualizarNombre
    data object Inesperado : ErrorActualizarNombre
}

sealed interface ResultadoActualizarNombre {
    data object Exito : ResultadoActualizarNombre
    data class Fallo(val error: ErrorActualizarNombre) : ResultadoActualizarNombre
}
