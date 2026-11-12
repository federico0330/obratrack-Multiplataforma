package com.unaj.obratrack.domain.validation

/** Reglas puras del nombre visible de la cuenta; se evalúa recortado. */
class ValidadorNombreUsuario {
    fun normalizar(nombre: String): String = nombre.trim()

    fun nombreValido(nombre: String): Boolean = normalizar(nombre).length in 1..LARGO_MAXIMO

    companion object {
        const val LARGO_MAXIMO = 60
    }
}
