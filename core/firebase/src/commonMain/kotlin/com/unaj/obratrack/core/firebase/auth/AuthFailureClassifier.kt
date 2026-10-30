package com.unaj.obratrack.core.firebase.auth

/** Tipo de excepción de GitLive, reducido a lo que importa para clasificar. */
internal enum class ThrowableKind { Network, TooManyRequests, InvalidCredentials, InvalidUser, Other }

/**
 * Clasificación pura (sin SDK) para poder testearla en commonTest.
 *
 * GitLive 2.7.0 no mapea igual en cada plataforma: en Android el código es el errorCode del SDK
 * ("ERROR_USER_DISABLED") y en iOS es el código numérico del NSError ("17005"), y en iOS
 * usuario inexistente, deshabilitado y demasiados intentos llegan como FirebaseAuthException
 * genérica. Por eso se mira el código primero y el tipo después.
 */
internal fun classifyAuthFailure(kind: ThrowableKind, code: String?): AuthFailure = when {
    code in USER_DISABLED_CODES -> AuthFailure.UserDisabled
    code in TOO_MANY_ATTEMPTS_CODES -> AuthFailure.TooManyAttempts
    code in INVALID_CREDENTIALS_CODES -> AuthFailure.InvalidCredentials
    kind == ThrowableKind.Network -> AuthFailure.NoConnection
    kind == ThrowableKind.TooManyRequests -> AuthFailure.TooManyAttempts
    kind == ThrowableKind.InvalidCredentials -> AuthFailure.InvalidCredentials
    kind == ThrowableKind.InvalidUser -> AuthFailure.InvalidCredentials
    else -> AuthFailure.Unexpected
}

private val USER_DISABLED_CODES = setOf("ERROR_USER_DISABLED", "17005")
private val TOO_MANY_ATTEMPTS_CODES = setOf("ERROR_TOO_MANY_REQUESTS", "17010")
private val INVALID_CREDENTIALS_CODES = setOf(
    "ERROR_INVALID_CREDENTIAL",
    "ERROR_WRONG_PASSWORD",
    "ERROR_USER_NOT_FOUND",
    "17004",
    "17009",
    "17011",
)
