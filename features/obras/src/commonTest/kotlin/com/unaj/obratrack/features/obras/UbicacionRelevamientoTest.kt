package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UbicacionRelevamientoTest {
    private val base = "https://www.google.com/maps/search/?api=1&query="

    @Test
    fun elEnlaceDeMapaLlevaLasCoordenadasConSeisDecimales() {
        assertEquals("$base-34.774600,-58.267200", enlaceMapa(Ubicacion(-34.7746, -58.2672)))
        assertEquals("${base}34.123457,-58.987654", enlaceMapa(Ubicacion(34.123456789, -58.987654321)))
        assertEquals("${base}0.000000,0.000000", enlaceMapa(Ubicacion(0.0, 0.0)))
    }

    @Test
    fun conservaLosCerosDeLaParteDecimalYNoUsaNotacionCientifica() {
        assertEquals("${base}0.000010,12.500000", enlaceMapa(Ubicacion(0.00001, 12.5)))
        assertEquals("${base}12.000070,100.001000", enlaceMapa(Ubicacion(12.00007, 100.001)))
        assertFalse(enlaceMapa(Ubicacion(0.00001, 0.00002)).contains('E'))
    }

    @Test
    fun unValorQueRedondeaACeroNoLlevaSigno() {
        assertEquals("${base}0.000000,1.500000", enlaceMapa(Ubicacion(-0.0000001, 1.5)))
    }

    @Test
    fun elEnlaceDeLosExtremosDelMundo() {
        assertEquals("${base}90.000000,180.000000", enlaceMapa(Ubicacion(90.0, 180.0)))
        assertEquals("${base}-90.000000,-180.000000", enlaceMapa(Ubicacion(-90.0, -180.0)))
    }

    @Test
    fun unValorNoFinitoSeMuestraTalCualEnVezDeFallar() {
        assertEquals("NaN", formatearGrados(Double.NaN, 6))
    }
}
