package com.unaj.obratrack.core.firebase.auth

/** Forma de transporte local al adaptador Firebase; no filtra tipos del SDK al dominio. */
data class AuthUserDto(val id: String, val email: String, val displayName: String?)
