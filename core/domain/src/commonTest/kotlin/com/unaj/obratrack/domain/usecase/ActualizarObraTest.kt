package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ActualizarObraTest {
    @Test
    fun validaNormalizaYActualizaConUidEId() = runTest {
        val repository = ActualizarObraRepositoryFake()
        val draft = ObraDraft(" Casa ", " Cliente ", " Calle 2 ", true, Instant.fromEpochMilliseconds(0))

        val result = ActualizarObra(repository)("uid-1", "obra-2", draft)

        assertEquals(ResultadoObra.Exito(Unit), result)
        assertEquals("uid-1", repository.userId)
        assertEquals("obra-2", repository.obraId)
        assertEquals(draft.copy(nombre = "Casa", cliente = "Cliente", direccion = "Calle 2"), repository.draft)
    }

    @Test
    fun permiteQuitarClienteYDireccion() = runTest {
        val repository = ActualizarObraRepositoryFake()
        val draft = ObraDraft(" Casa ", "  ", null, true, Instant.fromEpochMilliseconds(0))

        val result = ActualizarObra(repository)("uid-1", "obra-2", draft)

        assertEquals(ResultadoObra.Exito(Unit), result)
        assertEquals(draft.copy(nombre = "Casa", cliente = null, direccion = null), repository.draft)
    }

    @Test
    fun entradasInvalidasNoLlamanAlRepositorio() = runTest {
        val repository = ActualizarObraRepositoryFake()
        val result = ActualizarObra(repository)(
            "uid-1", "obra-2", ObraDraft(" ", "Cliente", "Calle", false, Instant.fromEpochMilliseconds(0)),
        )

        assertEquals(ResultadoObra.Fallo(ErrorObra.NOMBRE_VACIO), result)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun requiereSesionYObra() = runTest {
        val repository = ActualizarObraRepositoryFake()
        val draft = ObraDraft("Casa", "Cliente", "Calle", false, Instant.fromEpochMilliseconds(0))

        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), ActualizarObra(repository)("", "obra", draft))
        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), ActualizarObra(repository)("uid", "", draft))
        assertEquals(0, repository.updateCalls)
    }
}

private class ActualizarObraRepositoryFake : RepositorioObras {
    var updateCalls = 0
    var userId: String? = null
    var obraId: String? = null
    var draft: ObraDraft? = null

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> =
        flowOf(ResultadoObra.Exito(emptyList()))

    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)

    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft): ResultadoObra<Unit> {
        updateCalls++
        this.userId = userId
        this.obraId = obraId
        this.draft = draft
        return ResultadoObra.Exito(Unit)
    }

    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) =
        ResultadoObra.Exito(Unit)

    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: kotlin.time.Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> = error("Operación no usada en esta prueba")

    override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
}
