package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import kotlinx.coroutines.flow.Flow

/** Emite el usuario con sesión activa, o null si no hay sesión. */
class ObservarSesion(private val repositorio: RepositorioAutenticacion) {
    operator fun invoke(): Flow<Usuario?> = repositorio.usuarioActual
}
