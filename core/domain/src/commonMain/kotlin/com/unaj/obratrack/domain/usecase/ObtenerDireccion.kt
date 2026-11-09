package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.ProveedorDirecciones
import com.unaj.obratrack.domain.repository.ResultadoDireccion
import com.unaj.obratrack.domain.validation.ValidadorUbicacion

class ObtenerDireccion(private val proveedor: ProveedorDirecciones) {
    suspend operator fun invoke(ubicacion: Ubicacion): ResultadoDireccion<String> {
        // Unas coordenadas imposibles no tienen dirección: no se le consulta nada al sistema.
        if (!ValidadorUbicacion.esValida(ubicacion)) return ResultadoDireccion.SinResultados
        return when (val resultado = proveedor.direccionDe(ubicacion)) {
            is ResultadoDireccion.Exito -> {
                val texto = resultado.dato.trim()
                if (texto.isEmpty()) ResultadoDireccion.SinResultados else ResultadoDireccion.Exito(texto)
            }
            else -> resultado
        }
    }
}
