package com.unaj.obratrack.core.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

/** Adaptador de infraestructura. Firebase se inicializa en el host de cada plataforma. */
class GitLiveFirebaseAuthDataSource(
    private val auth: FirebaseAuth = Firebase.auth,
) : FirebaseAuthDataSource {
    override val currentUser = auth.authStateChanged.map { it?.toDto() }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUserDto> = try {
        val user = auth.signInWithEmailAndPassword(email, password).user
        if (user == null) AuthResult.Failure(AuthFailure.Unexpected) else AuthResult.Success(user.toDto())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        AuthResult.Failure(error.toAuthFailure())
    }

    override suspend fun signOut(): AuthResult<Unit> = try {
        auth.signOut()
        AuthResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        AuthResult.Failure(error.toAuthFailure())
    }

    private fun Exception.toAuthFailure(): AuthFailure {
        val kind = when (this) {
            is FirebaseNetworkException -> ThrowableKind.Network
            is FirebaseTooManyRequestsException -> ThrowableKind.TooManyRequests
            is FirebaseAuthInvalidCredentialsException -> ThrowableKind.InvalidCredentials
            is FirebaseAuthInvalidUserException -> ThrowableKind.InvalidUser
            else -> ThrowableKind.Other
        }
        return classifyAuthFailure(kind, (this as? FirebaseAuthException)?.code)
    }

    private fun dev.gitlive.firebase.auth.FirebaseUser.toDto() =
        AuthUserDto(uid, email.orEmpty(), displayName)
}
