package com.unaj.obratrack.domain.validation

/** Reglas puras de la descripción libre que se envía al servicio de sugerencias; se evalúa recortada. */
class ValidadorDescripcionObra {
    fun normalizar(descripcion: String): String = descripcion.trim()

    fun descripcionValida(descripcion: String): Boolean = normalizar(descripcion).length in 1..LARGO_MAXIMO

    companion object {
        const val LARGO_MAXIMO = 500
    }
}
