package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.ordenadas
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObservarTareas(private val repositorio: RepositorioTareas) {
    operator fun invoke(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> =
        if (userId.isBlank() || obraId.isBlank()) flowOf(ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA))
        else repositorio.observarDeObra(userId, obraId).map { resultado ->
            when (resultado) {
                is ResultadoTarea.Exito -> ResultadoTarea.Exito(resultado.dato.ordenadas())
                is ResultadoTarea.Fallo -> resultado
            }
        }
}
