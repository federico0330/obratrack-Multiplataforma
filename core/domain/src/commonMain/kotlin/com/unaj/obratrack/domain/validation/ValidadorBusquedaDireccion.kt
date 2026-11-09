package com.unaj.obratrack.domain.validation

/**
 * Regla pura: el texto de búsqueda se recorta y se unifican los espacios. No puede ser vacío ni demasiado
 * corto: se cuentan solo los caracteres que no son espacios, así "a b" no alcanza.
 */
object ValidadorBusquedaDireccion {
    const val LARGO_MINIMO = 3

    private val espacios = Regex("\\s+")

    /** El texto listo para buscar, o null si no alcanza. */
    fun normalizar(texto: String): String? =
        texto.trim().replace(espacios, " ").takeIf { consulta -> consulta.count { !it.isWhitespace() } >= LARGO_MINIMO }
}
