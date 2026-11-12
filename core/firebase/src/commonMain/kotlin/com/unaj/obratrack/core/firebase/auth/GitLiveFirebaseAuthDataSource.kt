package com.unaj.obratrack.core.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.FirebaseTooManyRequestsException
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthEmailException
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/** Adaptador de infraestructura. Firebase se inicializa en el host de cada plataforma. */
class GitLiveFirebaseAuthDataSource(
    private val auth: FirebaseAuth = Firebase.auth,
) : FirebaseAuthDataSource, FirebaseAccountDataSource {
    // authStateChanged no avisa cuando cambia el perfil (el nombre), así que se avisa a mano.
    private val perfilActualizado = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val currentUser = merge(
        auth.authStateChanged,
        perfilActualizado.map { auth.currentUser },
    ).map { it?.toDto() }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUserDto> = try {
        val user = auth.signInWithEmailAndPassword(email, password).user
        if (user == null) AuthResult.Failure(AuthFailure.Unexpected) else AuthResult.Success(user.toDto())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        AuthResult.Failure(error.toAuthFailure())
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult<AuthUserDto> = try {
        val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = null)
        val user = auth.signInWithCredential(credential).user
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

    override suspend fun signUp(email: String, password: String): RegistrationResult = try {
        val user = auth.createUserWithEmailAndPassword(email, password).user
        if (user == null) {
            RegistrationResult.Failure(RegistrationFailure.Unexpected)
        } else {
            RegistrationResult.Success(user.toDto())
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        RegistrationResult.Failure(classifyRegistrationFailure(error.toThrowableKind(), error.authCode()))
    }

    override suspend fun sendPasswordReset(email: String): AccountResult = try {
        auth.sendPasswordResetEmail(email)
        AccountResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (isUnknownAccountOnReset(error.authCode())) {
            AccountResult.Success
        } else {
            AccountResult.Failure(classifyAccountFailure(error.toThrowableKind(), error.authCode()))
        }
    }

    override suspend fun updateDisplayName(name: String): AccountResult = try {
        val user = auth.currentUser
        if (user == null) {
            AccountResult.Failure(AccountFailure.NoSession)
        } else {
            user.updateProfile(displayName = name)
            perfilActualizado.emit(Unit)
            AccountResult.Success
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        AccountResult.Failure(classifyAccountFailure(error.toThrowableKind(), error.authCode()))
    }

    private fun Exception.toAuthFailure(): AuthFailure =
        classifyAuthFailure(toThrowableKind(), authCode())

    private fun Exception.authCode(): String? = (this as? FirebaseAuthException)?.code

    // FirebaseAuthWeakPasswordException hereda de InvalidCredentials: va primero.
    private fun Exception.toThrowableKind(): ThrowableKind =
        when (this) {
            is FirebaseNetworkException -> ThrowableKind.Network
            is FirebaseTooManyRequestsException -> ThrowableKind.TooManyRequests
            is FirebaseAuthWeakPasswordException -> ThrowableKind.WeakPassword
            is FirebaseAuthInvalidCredentialsException -> ThrowableKind.InvalidCredentials
            is FirebaseAuthInvalidUserException -> ThrowableKind.InvalidUser
            is FirebaseAuthUserCollisionException -> ThrowableKind.UserCollision
            is FirebaseAuthEmailException -> ThrowableKind.InvalidEmail
            else -> ThrowableKind.Other
        }

    private fun dev.gitlive.firebase.auth.FirebaseUser.toDto() =
        AuthUserDto(uid, email.orEmpty(), displayName)
}
