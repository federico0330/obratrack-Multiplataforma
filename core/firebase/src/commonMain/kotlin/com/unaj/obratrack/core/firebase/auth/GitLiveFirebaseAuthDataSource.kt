package com.unaj.obratrack.core.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.map

/** Adaptador de infraestructura. Firebase se inicializa en el host de cada plataforma. */
class GitLiveFirebaseAuthDataSource(
    private val auth: FirebaseAuth = Firebase.auth,
) : FirebaseAuthDataSource {
    override val currentUser = auth.authStateChanged.map { it?.toDto() }

    override suspend fun signIn(email: String, password: String): AuthUserDto {
        val user = auth.signInWithEmailAndPassword(email, password).user
            ?: error("Authentication returned no user")
        return user.toDto()
    }

    override suspend fun signOut() = auth.signOut()

    private fun dev.gitlive.firebase.auth.FirebaseUser.toDto() =
        AuthUserDto(uid, email.orEmpty(), displayName)
}
