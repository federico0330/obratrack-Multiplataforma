package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface RepositorioAutenticacion {
    val usuarioActual: Flow<Usuario?>
    suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso
    suspend fun cerrarSesion()
}

/** Fallos conocidos en términos del producto; nunca expone texto/códigos del SDK. */
sealed interface ResultadoAcceso {
    data class Exito(val usuario: Usuario) : ResultadoAcceso
    data object CredencialesInvalidas : ResultadoAcceso
    data object SinConexion : ResultadoAcceso
    data object ErrorInesperado : ResultadoAcceso
}
