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
}

/** Solo el nombre es obligatorio; cliente y dirección son opcionales. */
class ValidadorObra {
    fun validar(draft: ObraDraft): ValidacionObra = when {
        draft.nombre.isBlank() -> ValidacionObra.NombreVacio
        else -> ValidacionObra.Valida
    }
}

/** Recorta los textos y deja en null cliente y dirección cuando quedan vacíos. */
internal fun ObraDraft.normalizado(): ObraDraft = copy(
    nombre = nombre.trim(),
    cliente = cliente?.trim()?.takeIf { it.isNotEmpty() },
    direccion = direccion?.trim()?.takeIf { it.isNotEmpty() },
)

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
        }
        return repositorio.crear(userId, draft.normalizado(), EstadoObra.ACTIVA)
    }
}
