package com.unaj.obratrack.core.firebase.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class RegistrationFailureClassifierTest {
    @Test
    fun emailEnUsoPorCodigoAndroid() = assertEquals(
        RegistrationFailure.EmailAlreadyInUse,
        classifyRegistrationFailure(ThrowableKind.UserCollision, "ERROR_EMAIL_ALREADY_IN_USE"),
    )

    @Test
    fun emailEnUsoPorCodigoIos() = assertEquals(
        RegistrationFailure.EmailAlreadyInUse,
        classifyRegistrationFailure(ThrowableKind.UserCollision, "17007"),
    )

    @Test
    fun emailEnUsoPorTipoSinCodigo() = assertEquals(
        RegistrationFailure.EmailAlreadyInUse,
        classifyRegistrationFailure(ThrowableKind.UserCollision, null),
    )

    @Test
    fun contrasenaDebilPorCodigoAndroid() = assertEquals(
        RegistrationFailure.WeakPassword,
        classifyRegistrationFailure(ThrowableKind.WeakPassword, "ERROR_WEAK_PASSWORD"),
    )

    @Test
    fun contrasenaDebilPorCodigoIos() = assertEquals(
        RegistrationFailure.WeakPassword,
        classifyRegistrationFailure(ThrowableKind.WeakPassword, "17026"),
    )

    @Test
    fun contrasenaDebilPorTipoSinCodigo() = assertEquals(
        RegistrationFailure.WeakPassword,
        classifyRegistrationFailure(ThrowableKind.WeakPassword, null),
    )

    @Test
    fun emailInvalidoEnAndroidLlegaComoCredencialesInvalidasConCodigo() = assertEquals(
        RegistrationFailure.InvalidEmail,
        classifyRegistrationFailure(ThrowableKind.InvalidCredentials, "ERROR_INVALID_EMAIL"),
    )

    @Test
    fun emailInvalidoPorCodigoIos() = assertEquals(
        RegistrationFailure.InvalidEmail,
        classifyRegistrationFailure(ThrowableKind.InvalidEmail, "17008"),
    )

    @Test
    fun credencialesInvalidasSinCodigoConocidoEsInesperado() = assertEquals(
        RegistrationFailure.Unexpected,
        classifyRegistrationFailure(ThrowableKind.InvalidCredentials, null),
    )

    @Test
    fun redSinCodigoEsSinConexion() = assertEquals(
        RegistrationFailure.NoConnection,
        classifyRegistrationFailure(ThrowableKind.Network, null),
    )

    @Test
    fun redPorCodigoAndroidEsSinConexion() = assertEquals(
        RegistrationFailure.NoConnection,
        classifyRegistrationFailure(ThrowableKind.Other, "ERROR_NETWORK_REQUEST_FAILED"),
    )

    @Test
    fun tooManyRequestsPorTipoEsDemasiadosIntentos() = assertEquals(
        RegistrationFailure.TooManyAttempts,
        classifyRegistrationFailure(ThrowableKind.TooManyRequests, null),
    )

    @Test
    fun tooManyRequestsPorCodigoAndroidEsDemasiadosIntentos() = assertEquals(
        RegistrationFailure.TooManyAttempts,
        classifyRegistrationFailure(ThrowableKind.Other, "ERROR_TOO_MANY_REQUESTS"),
    )

    @Test
    fun tooManyRequestsPorCodigoIosEsDemasiadosIntentos() = assertEquals(
        RegistrationFailure.TooManyAttempts,
        classifyRegistrationFailure(ThrowableKind.Other, "17010"),
    )

    @Test
    fun otroErrorEsInesperado() = assertEquals(
        RegistrationFailure.Unexpected,
        classifyRegistrationFailure(ThrowableKind.Other, null),
    )

    @Test
    fun codigoDesconocidoEsInesperado() = assertEquals(
        RegistrationFailure.Unexpected,
        classifyRegistrationFailure(ThrowableKind.Other, "ERROR_ALGO_NUEVO"),
    )
}
