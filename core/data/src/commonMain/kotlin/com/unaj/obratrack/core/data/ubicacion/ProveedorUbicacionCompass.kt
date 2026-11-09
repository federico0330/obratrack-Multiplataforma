package com.unaj.obratrack.core.data.ubicacion

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.ProveedorUbicacion
import com.unaj.obratrack.domain.repository.ResultadoUbicacion
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile
import dev.jordond.compass.permissions.LocationAccuracy
import dev.jordond.compass.permissions.LocationPermissionController
import dev.jordond.compass.permissions.PermissionState
import dev.jordond.compass.permissions.mobile
import kotlinx.coroutines.CancellationException

/**
 * Ubicación con Compass: en Android usa el `LocationManager` del sistema, sin Google Play Services, y en
 * iOS `CLLocationManager`. Pide primero la mejor precisión; si el usuario deja solo la aproximada
 * (Android 12 o más, o "Ubicación precisa" apagada en iOS) se sigue con esa.
 */
class ProveedorUbicacionCompass(
    private val geolocator: Geolocator,
    private val permisos: LocationPermissionController,
) : ProveedorUbicacion {
    override suspend fun solicitarAcceso(): ResultadoUbicacion.Fallo? = try {
        // Compass también revisa esto antes de pedir la posición; acá se hace antes del permiso para no
        // mostrar el diálogo con la ubicación del teléfono apagada.
        if (!geolocator.isAvailable()) {
            ResultadoUbicacion.Desactivada
        } else {
            when (permisos.requirePermissionFor(Priority.HighAccuracy)) {
                PermissionState.Granted, PermissionState.NotDetermined -> null
                // Con "Aproximada" Android responde Denied para el permiso preciso, pero el aproximado
                // quedó concedido: se puede seguir.
                PermissionState.Denied, PermissionState.DeniedForever ->
                    if (permisos.hasPermission()) null else ResultadoUbicacion.SinPermiso
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoUbicacion.NoDisponible
    }

    override suspend fun posicionActual(): ResultadoUbicacion = try {
        // Con solo la aproximada se pide con prioridad media: pedir HighAccuracy volvería a pedir el permiso.
        val prioridad = if (permisos.grantedAccuracy() == LocationAccuracy.Reduced) Priority.Balanced else Priority.HighAccuracy
        traducir(geolocator.current(prioridad))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ResultadoUbicacion.NoDisponible
    }
}

internal fun traducir(resultado: GeolocatorResult): ResultadoUbicacion = when (resultado) {
    is GeolocatorResult.Success ->
        ResultadoUbicacion.Exito(Ubicacion(resultado.data.coordinates.latitude, resultado.data.coordinates.longitude))
    is GeolocatorResult.PermissionDenied -> ResultadoUbicacion.SinPermiso
    // Compass usa NotSupported cuando el servicio de ubicación del teléfono está apagado.
    GeolocatorResult.NotSupported -> ResultadoUbicacion.Desactivada
    // Sin señal a tiempo, sin proveedor o un fallo del sistema.
    else -> ResultadoUbicacion.NoDisponible
}

/** Crea Compass recién al primer uso: en iOS construir el administrador de ubicación no es gratis. */
fun crearProveedorUbicacionCompass(): ProveedorUbicacion {
    val proveedor by lazy {
        val permisos = LocationPermissionController.mobile()
        ProveedorUbicacionCompass(Geolocator.mobile(permisos), permisos)
    }
    return object : ProveedorUbicacion {
        override suspend fun solicitarAcceso() = proveedor.solicitarAcceso()
        override suspend fun posicionActual() = proveedor.posicionActual()
    }
}
