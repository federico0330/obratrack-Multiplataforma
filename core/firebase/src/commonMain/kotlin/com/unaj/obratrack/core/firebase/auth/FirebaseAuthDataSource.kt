package com.unaj.obratrack.core.firebase.auth

import kotlinx.coroutines.flow.Flow

interface FirebaseAuthDataSource {
    val currentUser: Flow<AuthUserDto?>
    suspend fun signIn(email: String, password: String): AuthResult<AuthUserDto>
    suspend fun signOut(): AuthResult<Unit>
}

/** Fallos de auth ya clasificados dentro de core/firebase; el SDK no cruza esta frontera. */
enum class AuthFailure {
    InvalidCredentials,
    NoConnection,
    TooManyAttempts,
    UserDisabled,
    Unexpected,
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val failure: AuthFailure) : AuthResult<Nothing>
}
