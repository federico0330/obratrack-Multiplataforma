package com.unaj.obratrack.core.data.direcciones

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.domain.repository.ResultadoDireccion
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Place
import dev.jordond.compass.exception.NotFoundException
import dev.jordond.compass.exception.NotSupportedException
import dev.jordond.compass.geocoder.exception.GeocodeException
import dev.jordond.compass.geocoder.mobile.MobilePlatformGeocoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProveedorDireccionesCompassTest {
    private val ubicacion = Ubicacion(-34.7746, -58.2672)
    private val calchaqui = lugar(street = "Av. Calchaquí 6200")

    private fun proveedor(geocodificador: GeocodificadorFalso) = ProveedorDireccionesCompass(geocodificador)

    @Test
    fun laDireccionEsLaPrimeraConTexto() = runTest {
        val geocodificador = GeocodificadorFalso(lugares = listOf(lugar(), calchaqui, lugar(street = "Otra 1")))
        assertEquals(ResultadoDireccion.Exito("Av. Calchaquí 6200"), proveedor(geocodificador).direccionDe(ubicacion))
        assertEquals(listOf(ubicacion.latitud to ubicacion.longitud), geocodificador.reversas)
    }

    @Test
    fun sinLugaresOSinTextoEsSinResultados() = runTest {
        assertEquals(ResultadoDireccion.SinResultados, proveedor(GeocodificadorFalso()).direccionDe(ubicacion))
        assertEquals(ResultadoDireccion.SinResultados, proveedor(GeocodificadorFalso(lugares = listOf(lugar()))).direccionDe(ubicacion))
    }

    @Test
    fun sinGeocodificadorEsNoSoportadoYNoConsulta() = runTest {
        val geocodificador = GeocodificadorFalso(disponible = false, lugares = listOf(calchaqui))
        assertFalse(proveedor(geocodificador).estaSoportado())
        assertEquals(ResultadoDireccion.NoSoportado, proveedor(geocodificador).direccionDe(ubicacion))
        assertEquals(ResultadoDireccion.NoSoportado, proveedor(geocodificador).buscar("Calchaquí"))
        assertTrue(geocodificador.reversas.isEmpty() && geocodificador.busquedas.isEmpty())
        assertTrue(proveedor(GeocodificadorFalso()).estaSoportado())
    }

    @Test
    fun traduceCadaExcepcionDeCompassAunqueSeanThrowableYNoException() = runTest {
        val casos = listOf<Pair<Throwable, ResultadoDireccion<Nothing>>>(
            NotSupportedException() to ResultadoDireccion.NoSoportado,
            NotFoundException() to ResultadoDireccion.SinResultados,
            IllegalArgumentException("coordenadas") to ResultadoDireccion.SinResultados,
            GeocodeException("Service not Available") to ResultadoDireccion.NoDisponible,
            IllegalStateException("inesperado") to ResultadoDireccion.NoDisponible,
        )
        for ((fallo, esperado) in casos) {
            val proveedor = proveedor(GeocodificadorFalso(fallo = fallo))
            assertEquals(esperado, proveedor.direccionDe(ubicacion), "reverse con $fallo")
            assertEquals(esperado, proveedor.buscar("Calchaquí"), "búsqueda con $fallo")
        }
    }

    @Test
    fun laCancelacionSeRelanza() = runTest {
        val proveedor = proveedor(GeocodificadorFalso(fallo = CancellationException("cancelada")))
        assertFailsWith<CancellationException> { proveedor.direccionDe(ubicacion) }
        assertFailsWith<CancellationException> { proveedor.buscar("Calchaquí") }
    }

    @Test
    fun buscarDevuelveCadaLugarConSuTextoYSusCoordenadas() = runTest {
        val geocodificador = GeocodificadorFalso(
            lugares = listOf(lugar(street = "Av. Calchaquí 6200", latitud = -34.77, longitud = -58.26), lugar(), lugar(street = "Calchaquí 100", latitud = -34.5, longitud = -58.5)),
        )
        val resultado = proveedor(geocodificador).buscar("Calchaquí")
        assertEquals(
            ResultadoDireccion.Exito(listOf(
                CandidatoDireccion("Av. Calchaquí 6200", Ubicacion(-34.77, -58.26)),
                CandidatoDireccion("Calchaquí 100", Ubicacion(-34.5, -58.5)),
            )),
            resultado,
        )
        assertEquals(listOf("Calchaquí"), geocodificador.busquedas)
    }

    @Test
    fun buscarSinLugaresConTextoEsSinResultados() = runTest {
        assertEquals(ResultadoDireccion.SinResultados, proveedor(GeocodificadorFalso()).buscar("Calchaquí"))
        assertEquals(ResultadoDireccion.SinResultados, proveedor(GeocodificadorFalso(lugares = listOf(lugar()))).buscar("Calchaquí"))
    }

    @Test
    fun enAndroidLaLineaCompletaNoRepiteLocalidadProvinciaNiPais() {
        val android = lugar(
            street = "Av. Calchaquí 6200, B1888 Florencio Varela, Provincia de Buenos Aires, Argentina",
            locality = "Florencio Varela", administrativeArea = "Provincia de Buenos Aires", country = "Argentina",
        )
        assertEquals("Av. Calchaquí 6200, B1888 Florencio Varela, Provincia de Buenos Aires, Argentina", textoDeLugar(android))
    }

    @Test
    fun enIosSeCompletaLaCalleConLocalidadProvinciaYPais() {
        val ios = lugar(street = "Av. Calchaquí 6200", locality = "Florencio Varela", administrativeArea = "Buenos Aires", country = "Argentina")
        assertEquals("Av. Calchaquí 6200, Florencio Varela, Buenos Aires, Argentina", textoDeLugar(ios))
    }

    @Test
    fun sinCalleUsaLaCalleYElNumeroSueltosOElNombre() {
        assertEquals("Av. Calchaquí 6200, Florencio Varela", textoDeLugar(lugar(thoroughfare = "Av. Calchaquí", subThoroughfare = "6200", locality = "Florencio Varela")))
        assertEquals("Obelisco, Buenos Aires", textoDeLugar(lugar(name = "Obelisco", locality = "Buenos Aires")))
        assertEquals("Florencio Varela", textoDeLugar(lugar(name = "Florencio Varela", locality = "Florencio Varela")))
    }

    @Test
    fun unLugarSinNingunTextoNoTieneDireccion() {
        assertNull(textoDeLugar(lugar()))
        assertNull(textoDeLugar(lugar(street = "   ", locality = " ", country = "")))
        assertNull(candidatoDe(lugar()))
    }
}

private fun lugar(
    latitud: Double = -34.7746, longitud: Double = -58.2672,
    name: String? = null, street: String? = null, country: String? = null, administrativeArea: String? = null,
    locality: String? = null, thoroughfare: String? = null, subThoroughfare: String? = null,
) = Place(
    coordinates = Coordinates(latitud, longitud), name = name, street = street, isoCountryCode = null, country = country,
    postalCode = null, administrativeArea = administrativeArea, subAdministrativeArea = null, locality = locality,
    subLocality = null, thoroughfare = thoroughfare, subThoroughfare = subThoroughfare,
)

private class GeocodificadorFalso(
    private val disponible: Boolean = true,
    private val lugares: List<Place> = emptyList(),
    private val fallo: Throwable? = null,
) : MobilePlatformGeocoder {
    val reversas = mutableListOf<Pair<Double, Double>>()
    val busquedas = mutableListOf<String>()
    override fun isAvailable() = disponible
    override suspend fun forward(address: String): List<Coordinates> = lugares.map { it.coordinates }
    override suspend fun reverse(latitude: Double, longitude: Double): List<Place> {
        reversas += latitude to longitude
        fallo?.let { throw it }
        return lugares
    }
    override suspend fun placeFromAddress(address: String): List<Place> {
        busquedas += address
        fallo?.let { throw it }
        return lugares
    }
}
