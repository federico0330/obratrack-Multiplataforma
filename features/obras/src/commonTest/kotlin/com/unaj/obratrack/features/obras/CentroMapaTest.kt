package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Ubicacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CentroMapaTest {
    private val relevamiento = Ubicacion(-34.7746, -58.2672)
    private val obra = Ubicacion(-34.6, -58.4)

    @Test
    fun arrancaEnElLugarYaElegidoConZoomDeCalle() {
        assertEquals(CentroMapa(relevamiento, ZOOM_ELEGIR_CON_PUNTO), centroInicialDelSelector(relevamiento, obra))
        assertEquals(CentroMapa(relevamiento, ZOOM_ELEGIR_CON_PUNTO), centroInicialDelSelector(relevamiento, null))
    }

    @Test
    fun sinLugarElegidoArrancaEnLaObra() {
        assertEquals(CentroMapa(obra, ZOOM_ELEGIR_CON_PUNTO), centroInicialDelSelector(null, obra))
    }

    @Test
    fun sinNingunaPistaArrancaEnBuenosAiresConZoomDeCiudad() {
        assertEquals(CentroMapa(CENTRO_POR_DEFECTO, ZOOM_ELEGIR_SIN_PUNTO), centroInicialDelSelector(null, null))
    }

    @Test
    fun unaUbicacionInvalidaNoSirveDePuntoDePartida() {
        val imposible = Ubicacion(95.0, 0.0)
        assertEquals(CentroMapa(obra, ZOOM_ELEGIR_CON_PUNTO), centroInicialDelSelector(imposible, obra))
        assertEquals(CentroMapa(CENTRO_POR_DEFECTO, ZOOM_ELEGIR_SIN_PUNTO), centroInicialDelSelector(imposible, Ubicacion(Double.NaN, 0.0)))
    }

    @Test
    fun elCentroDeLaCamaraSeConvierteEnUbicacion() {
        assertEquals(Ubicacion(-34.7746, -58.2672), ubicacionDelCentro(-34.7746, -58.2672))
        assertEquals(Ubicacion(90.0, 180.0), ubicacionDelCentro(90.0, 180.0))
        assertEquals(Ubicacion(-90.0, -180.0), ubicacionDelCentro(-90.0, -180.0))
    }

    @Test
    fun laLongitudQueSePasoDeLaLineaDeCambioDeFechaVuelveAlRango() {
        assertEquals(Ubicacion(10.0, -179.0), ubicacionDelCentro(10.0, 181.0))
        assertEquals(Ubicacion(10.0, 179.0), ubicacionDelCentro(10.0, -181.0))
        assertEquals(Ubicacion(10.0, 20.0), ubicacionDelCentro(10.0, 380.0))
    }

    @Test
    fun unCentroInvalidoNoEsUnaUbicacion() {
        assertNull(ubicacionDelCentro(91.0, 0.0))
        assertNull(ubicacionDelCentro(-91.0, 0.0))
        assertNull(ubicacionDelCentro(Double.NaN, 0.0))
        assertNull(ubicacionDelCentro(0.0, Double.POSITIVE_INFINITY))
    }
}
