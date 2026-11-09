package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.Ubicacion

sealed interface ResultadoUbicacion {
    data class Exito(val ubicacion: Ubicacion) : ResultadoUbicacion

    sealed interface Fallo : ResultadoUbicacion

    /** El usuario no dio el permiso (o lo negó para siempre): hay que habilitarlo desde los ajustes. */
    data object SinPermiso : Fallo

    /** El servicio de ubicación del teléfono está apagado. */
    data object Desactivada : Fallo

    /** No se consiguió una posición: sin señal a tiempo, sin proveedor o un error del sistema. */
    data object NoDisponible : Fallo
}

/**
 * Ubicación del dispositivo. Son dos pasos porque pedir el permiso depende de lo que tarde el usuario
 * en decidir y no debe contar contra el tiempo máximo de la búsqueda.
 */
interface ProveedorUbicacion {
    /**
     * Comprueba que el servicio esté activo y que haya permiso, pidiéndolo si falta. Devuelve null si ya
     * se puede pedir la posición. Acepta la ubicación aproximada. Sin tiempo máximo.
     */
    suspend fun solicitarAcceso(): ResultadoUbicacion.Fallo?

    /** Busca la posición actual. Solo después de [solicitarAcceso]; el llamador le pone el tiempo máximo. */
    suspend fun posicionActual(): ResultadoUbicacion
}
