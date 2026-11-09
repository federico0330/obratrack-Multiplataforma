package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.ProveedorUbicacion
import com.unaj.obratrack.domain.repository.ResultadoUbicacion
import com.unaj.obratrack.domain.validation.ValidadorUbicacion
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ObtenerUbicacionActual(
    private val proveedor: ProveedorUbicacion,
    private val tiempoMaximo: Duration = 20.seconds,
) {
    suspend operator fun invoke(): ResultadoUbicacion {
        // El permiso queda fuera del tiempo máximo: depende de cuánto tarde el usuario en responder.
        proveedor.solicitarAcceso()?.let { return it }
        val resultado = withTimeoutOrNull(tiempoMaximo) { proveedor.posicionActual() }
            ?: return ResultadoUbicacion.NoDisponible
        // Una posición con coordenadas imposibles no es una ubicación: nunca se informa como éxito.
        return if (resultado is ResultadoUbicacion.Exito && !ValidadorUbicacion.esValida(resultado.ubicacion)) {
            ResultadoUbicacion.NoDisponible
        } else {
            resultado
        }
    }
}
