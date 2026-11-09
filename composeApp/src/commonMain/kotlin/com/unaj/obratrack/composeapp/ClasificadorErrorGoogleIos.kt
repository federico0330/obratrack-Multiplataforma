package com.unaj.obratrack.composeapp

import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle

/**
 * Clasificación pura (sin SDK) del NSError que devuelve Google Sign-In en iOS, para poder testearla
 * en commonTest. Mismo criterio que con Firebase: se mira dominio y código, nunca el texto.
 */
internal fun clasificarErrorGoogleIos(dominio: String, codigo: Long): ResultadoTokenGoogle = when {
    dominio == GID_SIGN_IN_DOMAIN && codigo == GID_CANCELED -> ResultadoTokenGoogle.Cancelado
    dominio == NS_URL_ERROR_DOMAIN -> ResultadoTokenGoogle.SinConexion
    dominio == APP_AUTH_GENERAL_DOMAIN && codigo == APP_AUTH_NETWORK_ERROR -> ResultadoTokenGoogle.SinConexion
    else -> ResultadoTokenGoogle.Fallo
}

private const val GID_SIGN_IN_DOMAIN = "com.google.GIDSignIn"
private const val GID_CANCELED = -5L
private const val NS_URL_ERROR_DOMAIN = "NSURLErrorDomain"
private const val APP_AUTH_GENERAL_DOMAIN = "org.openid.appauth.general"
private const val APP_AUTH_NETWORK_ERROR = -5L
