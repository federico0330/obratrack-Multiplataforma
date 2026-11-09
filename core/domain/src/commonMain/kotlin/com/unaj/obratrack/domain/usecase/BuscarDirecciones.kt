package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.CandidatoDireccion
import com.unaj.obratrack.domain.repository.ProveedorDirecciones
import com.unaj.obratrack.domain.repository.ResultadoDireccion
import com.unaj.obratrack.domain.validation.ValidadorBusquedaDireccion
import com.unaj.obratrack.domain.validation.ValidadorUbicacion

class BuscarDirecciones(
    private val proveedor: ProveedorDirecciones,
    private val maximo: Int = 5,
) {
    /** Si el dispositivo puede buscar direcciones; si no, la pantalla no ofrece el buscador. */
    fun estaSoportado(): Boolean = proveedor.estaSoportado()

    suspend operator fun invoke(texto: String): ResultadoDireccion<List<CandidatoDireccion>> {
        val consulta = ValidadorBusquedaDireccion.normalizar(texto) ?: return ResultadoDireccion.SinResultados
        return when (val resultado = proveedor.buscar(consulta)) {
            is ResultadoDireccion.Exito -> {
                val validos = resultado.dato
                    .map { it.copy(texto = it.texto.trim()) }
                    // Un candidato sin texto no se puede elegir, y uno con coordenadas imposibles no se puede guardar.
                    .filter { it.texto.isNotEmpty() && ValidadorUbicacion.esValida(it.ubicacion) }
                    .distinct()
                    .take(maximo)
                if (validos.isEmpty()) ResultadoDireccion.SinResultados else ResultadoDireccion.Exito(validos)
            }
            else -> resultado
        }
    }
}
