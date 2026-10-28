package com.unaj.obratrack.domain.validation

/** Resultado explícito para que la interfaz pueda mostrar errores sin conocer detalles técnicos. */
sealed interface ResultadoValidacion {
    data object Valido : ResultadoValidacion
    data object NombreVacio : ResultadoValidacion
    data object NombreDemasiadoLargo : ResultadoValidacion
}

/** Regla pura: misma entrada produce siempre el mismo resultado, sin red ni plataforma. */
class ValidadorNombreObra {
    fun validar(nombre: String): ResultadoValidacion {
        val normalizado = nombre.trim()
        return when {
            normalizado.isEmpty() -> ResultadoValidacion.NombreVacio
            normalizado.length > NOMBRE_MAXIMO -> ResultadoValidacion.NombreDemasiadoLargo
            else -> ResultadoValidacion.Valido
        }
    }

    private companion object {
        const val NOMBRE_MAXIMO = 80
    }
}
