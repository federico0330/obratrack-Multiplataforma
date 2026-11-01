package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra

sealed interface ValidacionObra {
    data object Valida : ValidacionObra
    data object NombreVacio : ValidacionObra
    data object ClienteVacio : ValidacionObra
    data object DireccionVacia : ValidacionObra
}

class ValidadorObra {
    fun validar(draft: ObraDraft): ValidacionObra = when {
        draft.nombre.isBlank() -> ValidacionObra.NombreVacio
        draft.cliente.isBlank() -> ValidacionObra.ClienteVacio
        draft.direccion.isBlank() -> ValidacionObra.DireccionVacia
        else -> ValidacionObra.Valida
    }
}

class CrearObra(
    private val repositorio: RepositorioObras,
    private val validador: ValidadorObra = ValidadorObra(),
) {
    suspend operator fun invoke(userId: String, draft: ObraDraft): ResultadoObra<Obra> {
        if (userId.isBlank()) return ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA)
        val validacion = validador.validar(draft)
        when (validacion) {
            ValidacionObra.Valida -> Unit
            ValidacionObra.NombreVacio -> return ResultadoObra.Fallo(ErrorObra.NOMBRE_VACIO)
            ValidacionObra.ClienteVacio -> return ResultadoObra.Fallo(ErrorObra.CLIENTE_VACIO)
            ValidacionObra.DireccionVacia -> return ResultadoObra.Fallo(ErrorObra.DIRECCION_VACIA)
        }
        val normalizado = draft.copy(
            nombre = draft.nombre.trim(),
            cliente = draft.cliente.trim(),
            direccion = draft.direccion.trim(),
        )
        return repositorio.crear(userId, normalizado, EstadoObra.ACTIVA)
    }
}
