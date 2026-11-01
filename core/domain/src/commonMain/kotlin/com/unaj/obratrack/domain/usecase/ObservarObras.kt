package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ObservarObras(private val repositorio: RepositorioObras) {
    operator fun invoke(userId: String): Flow<ResultadoObra<List<Obra>>> =
        if (userId.isBlank()) flowOf(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA))
        else repositorio.observarPropias(userId)
}
