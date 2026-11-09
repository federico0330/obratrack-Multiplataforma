package com.unaj.obratrack.domain.repository

/**
 * Guarda una foto fuera de Firestore y devuelve su URL pública. La implementación decide cómo
 * achicarla antes de subirla; una cola sin conexión puede implementar este mismo puerto.
 */
interface AlmacenFotos {
    suspend fun subir(bytes: ByteArray, carpeta: String, nombre: String): ResultadoSubidaFoto
}

sealed interface ResultadoSubidaFoto {
    data class Exito(val url: String) : ResultadoSubidaFoto
    data object SinConexion : ResultadoSubidaFoto
    /** No se pudo dejar la foto por debajo del tope de peso. */
    data object MuyGrande : ResultadoSubidaFoto
    data object Fallo : ResultadoSubidaFoto
}
