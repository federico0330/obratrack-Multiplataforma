package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface RepositorioAutenticacion {
    val usuarioActual: Flow<Usuario?>
    suspend fun iniciarSesion(email: String, clave: String): Usuario
    suspend fun cerrarSesion()
}
