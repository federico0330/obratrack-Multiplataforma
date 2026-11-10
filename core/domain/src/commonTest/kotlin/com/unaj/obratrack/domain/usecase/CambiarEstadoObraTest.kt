package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CambiarEstadoObraTest {
    @Test
    fun enviaUidIdYEstadoAlRepositorio() = runTest {
        val repository = CambiarEstadoRepositoryFake()

        val result = CambiarEstadoObra(repository)("uid-1", "obra-3", EstadoObra.PAUSADA)

        assertEquals(ResultadoObra.Exito(Unit), result)
        assertEquals("uid-1", repository.userId)
        assertEquals("obra-3", repository.obraId)
        assertEquals(EstadoObra.PAUSADA, repository.estado)
    }

    @Test
    fun noAceptaUidNiIdVacios() = runTest {
        val repository = CambiarEstadoRepositoryFake()
        assertEquals(
            ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA),
            CambiarEstadoObra(repository)(" ", "obra", EstadoObra.ACTIVA),
        )
        assertEquals(0, repository.calls)
    }
}

private class CambiarEstadoRepositoryFake : RepositorioObras {
    var calls = 0
    var userId: String? = null
    var obraId: String? = null
    var estado: EstadoObra? = null

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> = emptyFlow()
    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra): ResultadoObra<Unit> {
        calls++
        this.userId = userId
        this.obraId = obraId
        this.estado = estado
        return ResultadoObra.Exito(Unit)
    }

    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: kotlin.time.Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> = error("Operación no usada en esta prueba")

    override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
}
