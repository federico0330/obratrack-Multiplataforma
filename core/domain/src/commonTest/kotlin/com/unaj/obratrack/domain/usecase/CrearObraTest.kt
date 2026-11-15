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
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CrearObraTest {
    @Test
    fun validaYNormalizaAntesDeGuardarConIdRemotoYEstadoActivo() = runTest {
        val repo = RepositorioObrasFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        val result = CrearObra(repo)("uid-1", ObraDraft("  Escuela ", " Cliente ", " Calle 1 ", false, fecha))

        val expected = Obra("generado-1", "uid-1", "Escuela", "Cliente", "Calle 1", false, EstadoObra.ACTIVA, fecha)
        assertEquals(ResultadoObra.Exito(expected), result)
        assertEquals(1, repo.createCalls)
        assertEquals("uid-1", repo.lastUserId)
        assertEquals(expected.nombre, repo.created?.nombre)
    }

    @Test
    fun creaConSoloElNombre() = runTest {
        val repo = RepositorioObrasFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        val result = CrearObra(repo)("uid-1", ObraDraft(nombre = " Escuela ", publica = false, fechaInicio = fecha))

        val expected = Obra("generado-1", "uid-1", "Escuela", null, null, false, EstadoObra.ACTIVA, fecha)
        assertEquals(ResultadoObra.Exito(expected), result)
        assertEquals(1, repo.createCalls)
    }

    @Test
    fun clienteYDireccionEnBlancoSeGuardanComoNulos() = runTest {
        val repo = RepositorioObrasFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        CrearObra(repo)("uid-1", ObraDraft("Escuela", "  ", "", false, fecha))

        assertEquals(null, repo.created?.cliente)
        assertEquals(null, repo.created?.direccion)
    }

    @Test
    fun nombreVacioNoLlamaAlRepo() = runTest {
        val repo = RepositorioObrasFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        val result = CrearObra(repo)("uid-1", ObraDraft(" ", "C", "D", false, fecha))

        assertEquals(ResultadoObra.Fallo(ErrorObra.NOMBRE_VACIO), result)
        assertEquals(0, repo.createCalls)
    }

    @Test
    fun uidVacioSeRechaza() = runTest {
        val repo = RepositorioObrasFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")
        val result = CrearObra(repo)(" ", ObraDraft("N", "C", "D", false, fecha))

        assertEquals(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA), result)
        assertEquals(0, repo.createCalls)
    }
}

private class RepositorioObrasFalso : RepositorioObras {
    var createCalls = 0
    var lastUserId: String? = null
    var created: Obra? = null

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> =
        flowOf(ResultadoObra.Exito(listOfNotNull(created)))

    override suspend fun crear(
        userId: String,
        draft: ObraDraft,
        estado: EstadoObra,
    ): ResultadoObra<Obra> {
        createCalls++
        lastUserId = userId
        return ResultadoObra.Exito(
            Obra("generado-1", userId, draft.nombre, draft.cliente, draft.direccion, draft.publica, estado, draft.fechaInicio)
                .also { created = it },
        )
    }

    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) =
        ResultadoObra.Exito(Unit)

    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) =
        ResultadoObra.Exito(Unit)

    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: kotlin.time.Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> = error("Operación no usada en esta prueba")

    override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
}
