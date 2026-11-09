package com.unaj.obratrack.domain.validation

import com.unaj.obratrack.domain.model.Ubicacion

/** Regla pura: coordenadas finitas y dentro de los rangos geográficos (NaN e infinitos no pasan). */
object ValidadorUbicacion {
    fun esValida(ubicacion: Ubicacion): Boolean =
        ubicacion.latitud.isFinite() && ubicacion.longitud.isFinite() &&
            ubicacion.latitud in -90.0..90.0 && ubicacion.longitud in -180.0..180.0
}
