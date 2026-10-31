package com.unaj.obratrack.core.firebase.auth

/**
 * Clasificación pura del alta de usuario. Mismo criterio que [classifyAuthFailure]: primero el código
 * (Android: errorCode "ERROR_*"; iOS: código numérico del NSError) y después el tipo.
 *
 * Un FirebaseAuthInvalidCredentialsException sin código conocido no se adivina como email inválido:
 * queda como Unexpected.
 */
internal fun classifyRegistrationFailure(kind: ThrowableKind, code: String?): RegistrationFailure = when {
    code in EMAIL_IN_USE_CODES -> RegistrationFailure.EmailAlreadyInUse
    code in WEAK_PASSWORD_CODES -> RegistrationFailure.WeakPassword
    code in INVALID_EMAIL_CODES -> RegistrationFailure.InvalidEmail
    code in REGISTRATION_TOO_MANY_CODES -> RegistrationFailure.TooManyAttempts
    code in REGISTRATION_NETWORK_CODES -> RegistrationFailure.NoConnection
    kind == ThrowableKind.UserCollision -> RegistrationFailure.EmailAlreadyInUse
    kind == ThrowableKind.WeakPassword -> RegistrationFailure.WeakPassword
    kind == ThrowableKind.InvalidEmail -> RegistrationFailure.InvalidEmail
    kind == ThrowableKind.Network -> RegistrationFailure.NoConnection
    kind == ThrowableKind.TooManyRequests -> RegistrationFailure.TooManyAttempts
    else -> RegistrationFailure.Unexpected
}

private val EMAIL_IN_USE_CODES = setOf("ERROR_EMAIL_ALREADY_IN_USE", "17007")
private val WEAK_PASSWORD_CODES = setOf("ERROR_WEAK_PASSWORD", "17026")
private val INVALID_EMAIL_CODES = setOf("ERROR_INVALID_EMAIL", "17008")
private val REGISTRATION_TOO_MANY_CODES = setOf("ERROR_TOO_MANY_REQUESTS", "17010", "17052")
private val REGISTRATION_NETWORK_CODES = setOf("ERROR_NETWORK_REQUEST_FAILED", "17020")
