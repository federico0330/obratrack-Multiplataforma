package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.validation.ResultadoValidacion
import com.unaj.obratrack.domain.validation.ValidadorNombreObra

sealed interface ResultadoCrearObra {
    data class Creada(val obra: Obra) : ResultadoCrearObra
    data class Rechazada(val motivo: ResultadoValidacion) : ResultadoCrearObra
}

/** Caso de uso: valida la petición y solo persiste si cumple la regla del dominio. */
class CrearObra(
    private val repositorio: RepositorioObras,
    private val validador: ValidadorNombreObra = ValidadorNombreObra(),
) {
    suspend operator fun invoke(id: String, nombre: String): ResultadoCrearObra {
        val validacion = validador.validar(nombre)
        if (validacion != ResultadoValidacion.Valido) {
            return ResultadoCrearObra.Rechazada(validacion)
        }

        val obra = Obra(id = id, nombre = nombre.trim())
        repositorio.crear(obra)
        return ResultadoCrearObra.Creada(obra)
    }
}
