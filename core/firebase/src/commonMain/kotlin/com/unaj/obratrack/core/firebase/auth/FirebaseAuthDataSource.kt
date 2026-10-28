package com.unaj.obratrack.core.firebase.auth

import kotlinx.coroutines.flow.Flow

interface FirebaseAuthDataSource {
    val currentUser: Flow<AuthUserDto?>
    suspend fun signIn(email: String, password: String): AuthUserDto
    suspend fun signOut()
}
