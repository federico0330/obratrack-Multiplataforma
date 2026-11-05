package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObservarRelevamientos(private val repositorio: RepositorioRelevamientos) {
    operator fun invoke(userId: String, obraId: String): Flow<ResultadoRelevamiento<List<Relevamiento>>> =
        if (userId.isBlank() || obraId.isBlank()) flowOf(ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_AUTORIZADA))
        else repositorio.observarDeObra(userId, obraId).map { result ->
            when (result) {
                is ResultadoRelevamiento.Exito -> ResultadoRelevamiento.Exito(
                    result.dato.sortedWith(compareByDescending<Relevamiento> { it.fecha }.thenBy { it.id }),
                )
                is ResultadoRelevamiento.Fallo -> result
            }
        }
}
