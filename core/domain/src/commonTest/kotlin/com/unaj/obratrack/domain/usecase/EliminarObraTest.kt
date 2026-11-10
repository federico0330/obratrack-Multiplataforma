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

class EliminarObraTest {
    @Test
    fun enviaUidEIdAlRepositorio() = runTest {
        val repository = DeleteRepositoryFake()

        assertEquals(ResultadoObra.Exito(Unit), EliminarObra(repository)("uid-1", "obra-9"))
        assertEquals("uid-1", repository.userId)
        assertEquals("obra-9", repository.obraId)
    }

    @Test
    fun rechazaUidOIdVaciosSinLlamarAlRepositorio() = runTest {
        val repository = DeleteRepositoryFake()

        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), EliminarObra(repository)(" ", "obra-9"))
        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), EliminarObra(repository)("uid-1", ""))
        assertEquals(0, repository.calls)
    }

    @Test
    fun propagaFalloParcial() = runTest {
        val repository = DeleteRepositoryFake().apply { result = ResultadoObra.Fallo(ErrorObra.FALLO_PARCIAL) }

        assertEquals(ResultadoObra.Fallo(ErrorObra.FALLO_PARCIAL), EliminarObra(repository)("uid-1", "obra-9"))
    }
}

private class DeleteRepositoryFake : RepositorioObras {
    var calls = 0
    var userId: String? = null
    var obraId: String? = null
    var result: ResultadoObra<Unit> = ResultadoObra.Exito(Unit)

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> = emptyFlow()
    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) = ResultadoObra.Exito(Unit)

    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: kotlin.time.Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> = error("Operación no usada en esta prueba")

    override suspend fun eliminar(userId: String, obraId: String): ResultadoObra<Unit> {
        calls++
        this.userId = userId
        this.obraId = obraId
        return result
    }
}
