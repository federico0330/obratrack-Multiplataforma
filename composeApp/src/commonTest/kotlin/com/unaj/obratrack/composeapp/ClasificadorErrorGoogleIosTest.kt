package com.unaj.obratrack.composeapp

import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle
import kotlin.test.Test
import kotlin.test.assertEquals

class ClasificadorErrorGoogleIosTest {
    @Test
    fun cancelarElSelectorEsCancelado() =
        assertEquals(ResultadoTokenGoogle.Cancelado, clasificarErrorGoogleIos("com.google.GIDSignIn", -5))

    @Test
    fun errorDeUrlEsSinConexion() =
        assertEquals(ResultadoTokenGoogle.SinConexion, clasificarErrorGoogleIos("NSURLErrorDomain", -1009))

    @Test
    fun errorDeRedDeAppAuthEsSinConexion() =
        assertEquals(ResultadoTokenGoogle.SinConexion, clasificarErrorGoogleIos("org.openid.appauth.general", -5))

    @Test
    fun otroCodigoDeGoogleEsFallo() =
        assertEquals(ResultadoTokenGoogle.Fallo, clasificarErrorGoogleIos("com.google.GIDSignIn", -1))

    @Test
    fun dominioDesconocidoEsFallo() =
        assertEquals(ResultadoTokenGoogle.Fallo, clasificarErrorGoogleIos("otro", -5))
}
