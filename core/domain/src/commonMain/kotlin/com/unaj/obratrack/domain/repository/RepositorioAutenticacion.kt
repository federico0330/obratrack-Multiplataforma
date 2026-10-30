package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface RepositorioAutenticacion {
    val usuarioActual: Flow<Usuario?>
    suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso
    suspend fun cerrarSesion(): ResultadoCerrarSesion
}

/** Fallos conocidos en términos del producto; nunca expone texto/códigos del SDK. */
sealed interface ErrorAuth {
    data object CredencialesInvalidas : ErrorAuth
    data object SinConexion : ErrorAuth
    data object DemasiadosIntentos : ErrorAuth
    data object UsuarioDeshabilitado : ErrorAuth
    data object Inesperado : ErrorAuth
}

sealed interface ResultadoAcceso {
    data class Exito(val usuario: Usuario) : ResultadoAcceso
    data class Fallo(val error: ErrorAuth) : ResultadoAcceso
}

sealed interface ResultadoCerrarSesion {
    data object Exito : ResultadoCerrarSesion
    data class Fallo(val error: ErrorAuth) : ResultadoCerrarSesion
}
