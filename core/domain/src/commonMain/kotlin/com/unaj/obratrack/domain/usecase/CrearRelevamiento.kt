package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.CambioFoto
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.AlmacenFotos
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento

class CrearRelevamiento(
    private val repositorio: RepositorioRelevamientos,
    private val almacenFotos: AlmacenFotos,
    private val generarNombreFoto: () -> String = ::nombreFotoAleatorio,
) {
    suspend operator fun invoke(userId: String, obraId: String, draft: RelevamientoDraft): ResultadoRelevamiento<Relevamiento> {
        val error = validarRelevamiento(userId, obraId, draft)
        if (error != null) return ResultadoRelevamiento.Fallo(error)
        // Al crear no hay foto previa: quitarla equivale a no tener foto.
        val cambio = draft.cambioFoto as? CambioFoto.Nueva ?: CambioFoto.SinCambio
        val fotoUrl = when (val foto = resolverFoto(cambio, obraId, almacenFotos, generarNombreFoto)) {
            is FotoResuelta.Error -> return ResultadoRelevamiento.Fallo(foto.error)
            is FotoResuelta.Lista -> (foto.cambio as? CambioFotoUrl.Nueva)?.url
        }
        return repositorio.crear(userId, obraId, draft.copy(nota = draft.nota.trim(), cambioFoto = CambioFoto.SinCambio), fotoUrl)
    }
}
