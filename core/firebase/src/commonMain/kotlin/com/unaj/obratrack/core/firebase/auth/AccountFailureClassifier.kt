package com.unaj.obratrack.core.firebase.auth

/**
 * Clasificación pura de los fallos de cuenta. Mismo criterio que [classifyAuthFailure]: primero el
 * código (Android: errorCode "ERROR_*"; iOS: código numérico del NSError) y después el tipo.
 */
internal fun classifyAccountFailure(kind: ThrowableKind, code: String?): AccountFailure = when {
    code in ACCOUNT_INVALID_EMAIL_CODES -> AccountFailure.InvalidEmail
    code in ACCOUNT_TOO_MANY_CODES -> AccountFailure.TooManyAttempts
    code in ACCOUNT_NETWORK_CODES -> AccountFailure.NoConnection
    code in ACCOUNT_NO_SESSION_CODES -> AccountFailure.NoSession
    kind == ThrowableKind.InvalidEmail -> AccountFailure.InvalidEmail
    kind == ThrowableKind.TooManyRequests -> AccountFailure.TooManyAttempts
    kind == ThrowableKind.Network -> AccountFailure.NoConnection
    kind == ThrowableKind.InvalidUser -> AccountFailure.NoSession
    else -> AccountFailure.Unexpected
}

/**
 * Pedir el correo de restablecimiento para una dirección sin cuenta no es un error del usuario:
 * se responde igual que si existiera para no revelar qué correos están registrados.
 */
internal fun isUnknownAccountOnReset(code: String?): Boolean = code in UNKNOWN_ACCOUNT_CODES

private val ACCOUNT_INVALID_EMAIL_CODES = setOf("ERROR_INVALID_EMAIL", "17008")
private val ACCOUNT_TOO_MANY_CODES = setOf("ERROR_TOO_MANY_REQUESTS", "17010", "17052")
private val ACCOUNT_NETWORK_CODES = setOf("ERROR_NETWORK_REQUEST_FAILED", "17020")
private val ACCOUNT_NO_SESSION_CODES = setOf(
    "ERROR_USER_TOKEN_EXPIRED",
    "ERROR_USER_NOT_FOUND",
    "ERROR_USER_DISABLED",
    "17021",
    "17011",
    "17005",
)
private val UNKNOWN_ACCOUNT_CODES = setOf("ERROR_USER_NOT_FOUND", "17011")
