package com.unaj.obratrack.domain.validation

import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidadorUbicacionTest {
    private fun valida(lat: Double, lon: Double) = ValidadorUbicacion.esValida(Ubicacion(lat, lon))

    @Test
    fun aceptaCoordenadasDentroDeLosRangosIncluidosLosBordes() {
        assertTrue(valida(-34.7746, -58.2672))
        assertTrue(valida(0.0, 0.0))
        assertTrue(valida(90.0, 180.0))
        assertTrue(valida(-90.0, -180.0))
    }

    @Test
    fun rechazaLatitudYLongitudFueraDeRango() {
        assertFalse(valida(90.0001, 0.0))
        assertFalse(valida(-90.0001, 0.0))
        assertFalse(valida(0.0, 180.0001))
        assertFalse(valida(0.0, -180.0001))
    }

    @Test
    fun rechazaNaNEInfinitos() {
        for (invalido in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFalse(valida(invalido, 0.0), "latitud $invalido")
            assertFalse(valida(0.0, invalido), "longitud $invalido")
        }
    }
}
