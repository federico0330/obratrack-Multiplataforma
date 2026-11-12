package com.unaj.obratrack.core.firebase.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountFailureClassifierTest {
    @Test
    fun emailInvalidoPorCodigoAndroid() = assertEquals(
        AccountFailure.InvalidEmail,
        classifyAccountFailure(ThrowableKind.Other, "ERROR_INVALID_EMAIL"),
    )

    @Test
    fun emailInvalidoPorCodigoIos() = assertEquals(
        AccountFailure.InvalidEmail,
        classifyAccountFailure(ThrowableKind.Other, "17008"),
    )

    @Test
    fun emailInvalidoPorTipoSinCodigo() = assertEquals(
        AccountFailure.InvalidEmail,
        classifyAccountFailure(ThrowableKind.InvalidEmail, null),
    )

    @Test
    fun demasiadosIntentosPorCodigoYPorTipo() {
        assertEquals(AccountFailure.TooManyAttempts, classifyAccountFailure(ThrowableKind.Other, "17010"))
        assertEquals(AccountFailure.TooManyAttempts, classifyAccountFailure(ThrowableKind.TooManyRequests, null))
    }

    @Test
    fun sinConexionPorCodigoYPorTipo() {
        assertEquals(AccountFailure.NoConnection, classifyAccountFailure(ThrowableKind.Other, "17020"))
        assertEquals(AccountFailure.NoConnection, classifyAccountFailure(ThrowableKind.Network, null))
    }

    @Test
    fun sesionVencidaOUsuarioInexistenteEsSinSesion() {
        assertEquals(AccountFailure.NoSession, classifyAccountFailure(ThrowableKind.Other, "ERROR_USER_TOKEN_EXPIRED"))
        assertEquals(AccountFailure.NoSession, classifyAccountFailure(ThrowableKind.InvalidUser, null))
    }

    @Test
    fun desconocidoEsInesperado() = assertEquals(
        AccountFailure.Unexpected,
        classifyAccountFailure(ThrowableKind.Other, null),
    )

    @Test
    fun cuentaInexistenteAlRecuperarSeReconocePorCodigo() {
        assertTrue(isUnknownAccountOnReset("ERROR_USER_NOT_FOUND"))
        assertTrue(isUnknownAccountOnReset("17011"))
        assertFalse(isUnknownAccountOnReset("ERROR_INVALID_EMAIL"))
        assertFalse(isUnknownAccountOnReset(null))
    }
}
