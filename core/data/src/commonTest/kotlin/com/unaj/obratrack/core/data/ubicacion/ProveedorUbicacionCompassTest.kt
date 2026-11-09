package com.unaj.obratrack.core.data.ubicacion

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.ResultadoUbicacion
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Location
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.permissions.LocationAccuracy
import dev.jordond.compass.permissions.LocationPermissionController
import dev.jordond.compass.permissions.PermissionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProveedorUbicacionCompassTest {
    private fun proveedor(geolocator: GeolocatorFalso = GeolocatorFalso(), permisos: PermisosFalsos = PermisosFalsos()) =
        ProveedorUbicacionCompass(geolocator, permisos)

    @Test
    fun conServicioYPermisoSePuedeBuscarYSePideLaMejorPrecision() = runTest {
        val permisos = PermisosFalsos(estado = PermissionState.Granted)
        assertNull(proveedor(permisos = permisos).solicitarAcceso())
        assertEquals(listOf(Priority.HighAccuracy), permisos.pedidos)
    }

    @Test
    fun conLaUbicacionDelTelefonoApagadaNoPideElPermiso() = runTest {
        val permisos = PermisosFalsos()
        val resultado = proveedor(GeolocatorFalso(disponible = false), permisos).solicitarAcceso()
        assertEquals(ResultadoUbicacion.Desactivada, resultado)
        assertTrue(permisos.pedidos.isEmpty())
    }

    @Test
    fun permisoDenegadoSinNingunPermisoConcedidoEsSinPermiso() = runTest {
        for (estado in listOf(PermissionState.Denied, PermissionState.DeniedForever)) {
            val permisos = PermisosFalsos(estado = estado, concedido = false)
            assertEquals(ResultadoUbicacion.SinPermiso, proveedor(permisos = permisos).solicitarAcceso())
        }
    }

    @Test
    fun conSoloLaUbicacionAproximadaSeSigueAdelante() = runTest {
        // Android responde Denied al permiso preciso si el usuario elige "Aproximada", pero el aproximado quedó concedido.
        for (estado in listOf(PermissionState.Denied, PermissionState.DeniedForever)) {
            val permisos = PermisosFalsos(estado = estado, concedido = true, precision = LocationAccuracy.Reduced)
            assertNull(proveedor(permisos = permisos).solicitarAcceso())
        }
    }

    @Test
    fun conPrecisionCompletaBuscaConAltaPrecisionYConAproximadaConPrioridadMedia() = runTest {
        val completa = GeolocatorFalso()
        proveedor(completa, PermisosFalsos(precision = LocationAccuracy.Full)).posicionActual()
        assertEquals(listOf(Priority.HighAccuracy), completa.prioridades)

        val aproximada = GeolocatorFalso()
        proveedor(aproximada, PermisosFalsos(precision = LocationAccuracy.Reduced)).posicionActual()
        assertEquals(listOf(Priority.Balanced), aproximada.prioridades)
    }

    @Test
    fun laPosicionSeConvierteEnUbicacion() = runTest {
        val resultado = proveedor(GeolocatorFalso(resultado = exito(-34.7746, -58.2672))).posicionActual()
        assertEquals(ResultadoUbicacion.Exito(Ubicacion(-34.7746, -58.2672)), resultado)
    }

    @Test
    fun traduceCadaResultadoDeCompass() {
        assertEquals(ResultadoUbicacion.SinPermiso, traducir(GeolocatorResult.PermissionDenied(forever = false)))
        assertEquals(ResultadoUbicacion.SinPermiso, traducir(GeolocatorResult.PermissionDenied(forever = true)))
        assertEquals(ResultadoUbicacion.Desactivada, traducir(GeolocatorResult.NotSupported))
        assertEquals(ResultadoUbicacion.NoDisponible, traducir(GeolocatorResult.NotFound))
        assertEquals(ResultadoUbicacion.NoDisponible, traducir(GeolocatorResult.GeolocationFailed("sin proveedor")))
    }

    @Test
    fun unaExcepcionInesperadaEsNoDisponibleYNuncaUnExito() = runTest {
        val roto = IllegalStateException("PermissionRequester is not available")
        assertEquals(ResultadoUbicacion.NoDisponible, proveedor(GeolocatorFalso(fallo = roto)).posicionActual())
        assertEquals(ResultadoUbicacion.NoDisponible, proveedor(permisos = PermisosFalsos(fallo = roto)).solicitarAcceso())
    }

    @Test
    fun laCancelacionSeRelanzaEnLosDosPasos() = runTest {
        val cancelacion = CancellationException("cancelada")
        assertFailsWith<CancellationException> { proveedor(GeolocatorFalso(fallo = cancelacion)).posicionActual() }
        assertFailsWith<CancellationException> { proveedor(permisos = PermisosFalsos(fallo = cancelacion)).solicitarAcceso() }
    }
}

private fun exito(latitud: Double, longitud: Double) = GeolocatorResult.Success(
    Location(Coordinates(latitud, longitud), 12.0, null, null, null, null, 0L),
)

private class GeolocatorFalso(
    private val disponible: Boolean = true,
    private val resultado: GeolocatorResult = exito(-34.0, -58.0),
    private val fallo: Throwable? = null,
) : Geolocator {
    val prioridades = mutableListOf<Priority>()
    override val trackingStatus: Flow<TrackingStatus> = emptyFlow()
    override suspend fun isAvailable() = disponible
    override suspend fun current(priority: Priority): GeolocatorResult {
        prioridades += priority
        fallo?.let { throw it }
        return resultado
    }
    override suspend fun current(request: LocationRequest) = current(request.priority)
    override suspend fun lastLocation(priority: Priority) = resultado
    override suspend fun lastLocation(request: LocationRequest) = resultado
    override fun track(request: LocationRequest): Flow<TrackingStatus> = trackingStatus
    override fun stopTracking() = Unit
}

private class PermisosFalsos(
    private val estado: PermissionState = PermissionState.Granted,
    private val concedido: Boolean = true,
    private val precision: LocationAccuracy = LocationAccuracy.Full,
    private val fallo: Throwable? = null,
) : LocationPermissionController {
    val pedidos = mutableListOf<Priority>()
    override fun hasPermission() = concedido
    override fun grantedAccuracy() = precision
    override suspend fun requirePermissionFor(priority: Priority): PermissionState {
        pedidos += priority
        fallo?.let { throw it }
        return estado
    }
}
