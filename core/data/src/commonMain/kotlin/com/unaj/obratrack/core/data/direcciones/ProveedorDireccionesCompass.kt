package com.unaj.obratrack.core.data.direcciones

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.domain.repository.ProveedorDirecciones
import com.unaj.obratrack.domain.repository.ResultadoDireccion
import dev.jordond.compass.Place
import dev.jordond.compass.exception.NotFoundException
import dev.jordond.compass.exception.NotSupportedException
import dev.jordond.compass.geocoder.exception.GeocodeException
import dev.jordond.compass.geocoder.mobile.MobilePlatformGeocoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Direcciones con el geocodificador del sistema (`Geocoder` en Android, `CLGeocoder` en iOS) a través de Compass.
 * Se usa la capa de plataforma directamente: `Geocoder.forward` de Compass devuelve solo coordenadas, sin el texto.
 */
class ProveedorDireccionesCompass(
    private val plataforma: MobilePlatformGeocoder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProveedorDirecciones {
    override fun estaSoportado(): Boolean = plataforma.isAvailable()

    override suspend fun direccionDe(ubicacion: Ubicacion): ResultadoDireccion<String> = consultar {
        plataforma.reverse(ubicacion.latitud, ubicacion.longitud).firstNotNullOfOrNull(::textoDeLugar)
    }

    override suspend fun buscar(texto: String): ResultadoDireccion<List<CandidatoDireccion>> = consultar {
        plataforma.placeFromAddress(texto).mapNotNull(::candidatoDe).takeIf { it.isNotEmpty() }
    }

    /**
     * Las excepciones de Compass heredan de `Throwable`, no de `Exception`: hay que atraparlas por tipo. El
     * sistema no informa por qué falló (el tipo del error se pierde y queda solo un texto), así que cualquier
     * otro fallo es `NoDisponible`.
     */
    private suspend fun <T : Any> consultar(consulta: suspend () -> T?): ResultadoDireccion<T> = try {
        if (!plataforma.isAvailable()) {
            ResultadoDireccion.NoSoportado
        } else {
            withContext(dispatcher) { consulta() }?.let { ResultadoDireccion.Exito(it) } ?: ResultadoDireccion.SinResultados
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: NotSupportedException) {
        ResultadoDireccion.NoSoportado
    } catch (_: NotFoundException) {
        ResultadoDireccion.SinResultados
    } catch (_: IllegalArgumentException) {
        // Compass las usa para coordenadas imposibles.
        ResultadoDireccion.SinResultados
    } catch (_: GeocodeException) {
        ResultadoDireccion.NoDisponible
    } catch (_: Exception) {
        ResultadoDireccion.NoDisponible
    }
}

/**
 * Texto para mostrar un lugar. En Android `street` ya es la línea completa ("Av. X 100, Ciudad, Provincia, País") y
 * en iOS solo calle y número: se agrega la localidad, la provincia y el país salvo que ya estén en el texto.
 */
internal fun textoDeLugar(lugar: Place): String? {
    fun limpio(valor: String?) = valor?.trim()?.takeIf { it.isNotEmpty() }
    val calle = limpio(lugar.street)
        ?: listOfNotNull(limpio(lugar.thoroughfare), limpio(lugar.subThoroughfare)).joinToString(" ").takeIf { it.isNotEmpty() }
    val partes = mutableListOf<String>()
    fun agregar(valor: String?) {
        val parte = limpio(valor) ?: return
        if (partes.none { it.contains(parte, ignoreCase = true) }) partes += parte
    }
    if (calle != null) partes += calle else agregar(lugar.name)
    agregar(lugar.locality)
    agregar(lugar.administrativeArea)
    agregar(lugar.country)
    return partes.joinToString(", ").takeIf { it.isNotEmpty() }
}

internal fun candidatoDe(lugar: Place): CandidatoDireccion? = textoDeLugar(lugar)?.let { texto ->
    CandidatoDireccion(texto, Ubicacion(lugar.coordinates.latitude, lugar.coordinates.longitude))
}

/** Compass se crea recién al primer uso: en Android hace falta que ya esté el contexto de la aplicación. */
fun crearProveedorDireccionesCompass(): ProveedorDirecciones {
    val proveedor by lazy { ProveedorDireccionesCompass(MobilePlatformGeocoder()) }
    return object : ProveedorDirecciones {
        override fun estaSoportado() = proveedor.estaSoportado()
        override suspend fun direccionDe(ubicacion: Ubicacion) = proveedor.direccionDe(ubicacion)
        override suspend fun buscar(texto: String) = proveedor.buscar(texto)
    }
}
