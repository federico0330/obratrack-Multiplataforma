package com.unaj.obratrack.core.firebase.auth

/**
 * Operaciones sobre la cuenta, separadas de [FirebaseAuthDataSource] para no obligar a los fakes
 * de acceso a implementarlas.
 */
interface FirebaseAccountDataSource {
    suspend fun sendPasswordReset(email: String): AccountResult
    suspend fun updateDisplayName(name: String): AccountResult
}

/** Fallos de cuenta ya clasificados dentro de core/firebase. */
enum class AccountFailure {
    InvalidEmail,
    NoSession,
    NoConnection,
    TooManyAttempts,
    Unexpected,
}

sealed interface AccountResult {
    data object Success : AccountResult
    data class Failure(val failure: AccountFailure) : AccountResult
}
