package com.unaj.obratrack.data.auth

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.map

/** Adaptador de salida: traduce Firebase al contrato que conoce el dominio. */
class FirebaseRepositorioAutenticacion : RepositorioAutenticacion {
    private val auth = Firebase.auth

    override val usuarioActual = auth.authStateChanged.map { user ->
        user?.let { Usuario(it.uid, it.email.orEmpty(), it.displayName) }
    }

    override suspend fun iniciarSesion(email: String, clave: String): Usuario {
        val user = auth.signInWithEmailAndPassword(email, clave).user
            ?: error("Firebase no devolvió un usuario")
        return Usuario(user.uid, user.email.orEmpty(), user.displayName)
    }

    override suspend fun cerrarSesion() = auth.signOut()
}
