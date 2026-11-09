package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UbicacionRelevamientoTest {
    @Test
    fun formateaLatitudYLongitudConCincoDecimales() {
        assertEquals("-34.77460, -58.26720", formatearCoordenadas(Ubicacion(-34.7746, -58.2672)))
        assertEquals("34.12346, -58.98765", formatearCoordenadas(Ubicacion(34.123456789, -58.987654321)))
        assertEquals("0.00000, 0.00000", formatearCoordenadas(Ubicacion(0.0, 0.0)))
    }

    @Test
    fun conservaLosCerosDeLaParteDecimalYNoUsaNotacionCientifica() {
        assertEquals("0.00001, 1.50000", formatearCoordenadas(Ubicacion(0.00001, 1.5)))
        assertEquals("12.00007, 100.00100", formatearCoordenadas(Ubicacion(12.00007, 100.001)))
        assertFalse(formatearCoordenadas(Ubicacion(0.00001, 0.00002)).contains('E'))
    }

    @Test
    fun unValorQueRedondeaACeroNoLlevaSigno() {
        assertEquals("0.00000, 1.50000", formatearCoordenadas(Ubicacion(-0.000001, 1.5)))
    }

    @Test
    fun formateaLosExtremosDelMundo() {
        assertEquals("90.00000, 180.00000", formatearCoordenadas(Ubicacion(90.0, 180.0)))
        assertEquals("-90.00000, -180.00000", formatearCoordenadas(Ubicacion(-90.0, -180.0)))
    }

    @Test
    fun unValorNoFinitoSeMuestraTalCualEnVezDeFallar() {
        assertEquals("NaN", formatearGrados(Double.NaN, 5))
    }

    @Test
    fun elEnlaceDeMapaLlevaLasCoordenadasConSeisDecimales() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=-34.774600,-58.267200",
            enlaceMapa(Ubicacion(-34.7746, -58.2672)),
        )
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=0.000010,12.500000",
            enlaceMapa(Ubicacion(0.00001, 12.5)),
        )
    }
}
