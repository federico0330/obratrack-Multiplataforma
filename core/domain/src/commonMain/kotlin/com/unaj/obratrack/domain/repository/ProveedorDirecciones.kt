package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Ubicacion

/** Un lugar encontrado al buscar una dirección: el texto para mostrarlo y dónde queda. */
data class CandidatoDireccion(val texto: String, val ubicacion: Ubicacion)

sealed interface ResultadoDireccion<out T> {
    data class Exito<T>(val dato: T) : ResultadoDireccion<T>

    /** El geocodificador respondió, pero no hay nada para esa consulta. */
    data object SinResultados : ResultadoDireccion<Nothing>

    /**
     * No se pudo consultar: sin conexión, sin respuesta del servicio del sistema u otro fallo. El
     * geocodificador del sistema no informa el motivo, así que no se distingue la falta de conexión.
     */
    data object NoDisponible : ResultadoDireccion<Nothing>

    /** El dispositivo no tiene geocodificador: no es un error, simplemente no se ofrece. */
    data object NoSoportado : ResultadoDireccion<Nothing>
}

/**
 * Direcciones del dispositivo. La dirección nunca se guarda: solo se usa para mostrarla y para elegir
 * una ubicación.
 */
interface ProveedorDirecciones {
    /** Si el dispositivo puede geocodificar. Es una consulta barata: no usa la red. */
    fun estaSoportado(): Boolean

    /** Dirección de unas coordenadas. */
    suspend fun direccionDe(ubicacion: Ubicacion): ResultadoDireccion<String>

    /** Lugares que coinciden con un texto. [texto] ya viene normalizado. */
    suspend fun buscar(texto: String): ResultadoDireccion<List<CandidatoDireccion>>
}
