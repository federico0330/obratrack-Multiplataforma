package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface RepositorioAutenticacion {
    val usuarioActual: Flow<Usuario?>
    suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso
    suspend fun cerrarSesion(): ResultadoCerrarSesion
    suspend fun registrar(email: String, contrasena: String): ResultadoRegistro
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

/** Fallos de registro en términos del producto; nunca expone texto/códigos del SDK. */
sealed interface ErrorRegistro {
    data object EmailYaEnUso : ErrorRegistro
    data object ContrasenaDebil : ErrorRegistro
    data object EmailInvalido : ErrorRegistro
    data object SinConexion : ErrorRegistro
    data object DemasiadosIntentos : ErrorRegistro
    data object Inesperado : ErrorRegistro
}

sealed interface ResultadoRegistro {
    data class Exito(val usuario: Usuario) : ResultadoRegistro
    data class Fallo(val error: ErrorRegistro) : ResultadoRegistro
}
