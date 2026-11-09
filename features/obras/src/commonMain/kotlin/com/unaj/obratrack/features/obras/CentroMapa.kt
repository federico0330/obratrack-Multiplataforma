package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.validation.ValidadorUbicacion

/** Buenos Aires: dónde arranca el mapa para elegir un lugar cuando no hay ninguna pista. */
internal val CENTRO_POR_DEFECTO = Ubicacion(-34.6037, -58.3816)

/** Estilo de OpenFreeMap: gratis y sin clave. El crédito lo muestra el propio mapa. */
internal const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"

/** Se ve la manzana y las calles de alrededor. */
internal const val ZOOM_VISTA_PREVIA = 15.0
internal const val ZOOM_ELEGIR_CON_PUNTO = 16.0

/** Se ve el área metropolitana, para ubicarse sin un punto de partida. */
internal const val ZOOM_ELEGIR_SIN_PUNTO = 11.0

internal data class CentroMapa(val ubicacion: Ubicacion, val zoom: Double)

/** Dónde arranca el mapa para elegir: el lugar ya elegido, si no el de la obra y si no Buenos Aires. */
internal fun centroInicialDelSelector(actual: Ubicacion?, obra: Ubicacion?): CentroMapa {
    val punto = actual?.takeIf { ValidadorUbicacion.esValida(it) } ?: obra?.takeIf { ValidadorUbicacion.esValida(it) }
    return if (punto != null) CentroMapa(punto, ZOOM_ELEGIR_CON_PUNTO) else CentroMapa(CENTRO_POR_DEFECTO, ZOOM_ELEGIR_SIN_PUNTO)
}

/**
 * Centro de la cámara convertido en ubicación. Al arrastrar el mapa más allá de la línea de cambio de fecha la
 * longitud se pasa de 180 o de -180: se lleva de nuevo al rango válido. Devuelve null si no es una ubicación válida.
 */
internal fun ubicacionDelCentro(latitud: Double, longitud: Double): Ubicacion? {
    if (!latitud.isFinite() || !longitud.isFinite()) return null
    val enRango = if (longitud in -180.0..180.0) longitud else ((longitud + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return Ubicacion(latitud, enRango).takeIf { ValidadorUbicacion.esValida(it) }
}
