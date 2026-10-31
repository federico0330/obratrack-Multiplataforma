package com.unaj.obratrack.domain.validation

/** Reglas puras del formulario de registro; el email se evalúa recortado. */
class ValidadorCredencialesRegistro {
    fun emailValido(email: String): Boolean {
        val normalizado = email.trim()
        if (normalizado.any { it.isWhitespace() }) return false
        val partes = normalizado.split('@')
        if (partes.size != 2) return false
        val (local, dominio) = partes
        if (local.isEmpty()) return false
        val etiquetas = dominio.split('.')
        return etiquetas.size >= 2 && etiquetas.all { it.isNotEmpty() }
    }

    fun contrasenaValida(contrasena: String): Boolean =
        contrasena.isNotBlank() && contrasena.length >= CONTRASENA_MINIMA

    private companion object {
        const val CONTRASENA_MINIMA = 6
    }
}
