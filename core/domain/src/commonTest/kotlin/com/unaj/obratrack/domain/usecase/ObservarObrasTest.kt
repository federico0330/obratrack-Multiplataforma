package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ObservarObrasTest {
    @Test
    fun consultaPropiasPasaElUidAlRepositorio() = runTest {
        val repo = RepositorioObrasFalsoLista()
        val result = ObservarObras(repo)("uid-42").single()

        assertEquals("uid-42", repo.requestedUserId)
        assertEquals(ResultadoObra.Exito(emptyList()), result)
    }

    @Test
    fun uidVacioProduceErrorSinConsultarRepositorio() = runTest {
        val repo = RepositorioObrasFalsoLista()
        val result = ObservarObras(repo)(" ").single()

        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), result)
        assertEquals(null, repo.requestedUserId)
    }
}

private class RepositorioObrasFalsoLista : RepositorioObras {
    var requestedUserId: String? = null
    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> {
        requestedUserId = userId
        return flowOf(ResultadoObra.Exito(emptyList()))
    }

    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)

    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) =
        ResultadoObra.Exito(Unit)

    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) =
        ResultadoObra.Exito(Unit)
}
