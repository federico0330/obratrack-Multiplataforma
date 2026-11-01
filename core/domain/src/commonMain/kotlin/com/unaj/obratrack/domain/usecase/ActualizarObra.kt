package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra

class ActualizarObra(
    private val repositorio: RepositorioObras,
    private val validador: ValidadorObra = ValidadorObra(),
) {
    suspend operator fun invoke(userId: String, obraId: String, draft: ObraDraft): ResultadoObra<Unit> {
        if (userId.isBlank() || obraId.isBlank()) {
            return ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA)
        }
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
        return repositorio.actualizar(userId, obraId, normalizado)
    }
}
