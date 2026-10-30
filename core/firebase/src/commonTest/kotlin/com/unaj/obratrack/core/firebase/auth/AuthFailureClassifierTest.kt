package com.unaj.obratrack.core.firebase.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthFailureClassifierTest {
    @Test
    fun redSinCodigoEsSinConexion() =
        assertEquals(AuthFailure.NoConnection, classifyAuthFailure(ThrowableKind.Network, null))

    @Test
    fun tooManyRequestsPorTipoEsDemasiadosIntentos() =
        assertEquals(AuthFailure.TooManyAttempts, classifyAuthFailure(ThrowableKind.TooManyRequests, null))

    @Test
    fun tooManyRequestsPorCodigoAndroidEsDemasiadosIntentos() =
        assertEquals(AuthFailure.TooManyAttempts, classifyAuthFailure(ThrowableKind.Other, "ERROR_TOO_MANY_REQUESTS"))

    @Test
    fun tooManyRequestsPorCodigoIosEsDemasiadosIntentos() =
        assertEquals(AuthFailure.TooManyAttempts, classifyAuthFailure(ThrowableKind.Other, "17010"))

    @Test
    fun usuarioDeshabilitadoPorCodigoAndroid() =
        assertEquals(AuthFailure.UserDisabled, classifyAuthFailure(ThrowableKind.InvalidUser, "ERROR_USER_DISABLED"))

    @Test
    fun usuarioDeshabilitadoPorCodigoIosLlegaComoExcepcionGenerica() =
        assertEquals(AuthFailure.UserDisabled, classifyAuthFailure(ThrowableKind.Other, "17005"))

    @Test
    fun credencialesInvalidasPorTipo() =
        assertEquals(AuthFailure.InvalidCredentials, classifyAuthFailure(ThrowableKind.InvalidCredentials, null))

    @Test
    fun usuarioInexistenteEsCredencialesInvalidas() =
        assertEquals(AuthFailure.InvalidCredentials, classifyAuthFailure(ThrowableKind.InvalidUser, "ERROR_USER_NOT_FOUND"))

    @Test
    fun usuarioInexistenteIosGenericoEsCredencialesInvalidas() =
        assertEquals(AuthFailure.InvalidCredentials, classifyAuthFailure(ThrowableKind.Other, "17011"))

    @Test
    fun otroErrorEsInesperado() =
        assertEquals(AuthFailure.Unexpected, classifyAuthFailure(ThrowableKind.Other, null))

    @Test
    fun codigoDesconocidoEsInesperado() =
        assertEquals(AuthFailure.Unexpected, classifyAuthFailure(ThrowableKind.Other, "ERROR_ALGO_NUEVO"))
}
