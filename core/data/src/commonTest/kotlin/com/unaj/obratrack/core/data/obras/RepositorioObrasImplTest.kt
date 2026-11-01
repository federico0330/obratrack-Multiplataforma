package com.unaj.obratrack.core.data.obras

import com.unaj.obratrack.core.firebase.obras.FirebaseObraDataSource
import com.unaj.obratrack.core.firebase.obras.FirebaseObraFailure
import com.unaj.obratrack.core.firebase.obras.FirebaseObraResult
import com.unaj.obratrack.core.firebase.obras.ObraDto
import com.unaj.obratrack.core.firebase.obras.ObraUpdateFields
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.ResultadoObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioObrasImplTest {
    @Test
    fun mapeaDocumentoYCamposOpcionalesAusentes() = runTest {
        val source = ObrasRemoteFalso()
        source.listResult = FirebaseObraResult.Success(listOf(dto()))

        val result = RepositorioObrasImpl(source).observarPropias("uid-1").first()

        assertEquals("uid-1", source.queriedUserId)
        assertEquals(ResultadoObra.Exito(listOf(
            com.unaj.obratrack.domain.model.Obra(
                id = "obra-1", userId = "uid-1", nombre = "Escuela", cliente = "Municipio",
                direccion = "Calle 1", publica = false, estado = EstadoObra.ACTIVA,
                fechaInicio = Instant.parse("2026-11-01T00:00:00Z"),
            ),
        )), result)
    }

    @Test
    fun creaConUidEstadoFechaYDevuelveIdRemoto() = runTest {
        val source = ObrasRemoteFalso()
        source.createResult = FirebaseObraResult.Success(dto(id = "remoto-7"))
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        val result = RepositorioObrasImpl(source).crear(
            "uid-2", ObraDraft("N", "C", "D", true, fecha), EstadoObra.ACTIVA,
        )

        assertEquals("uid-2", source.createdDto?.userId)
        assertEquals("activa", source.createdDto?.estado)
        assertEquals(fecha.toEpochMilliseconds(), source.createdDto?.fechaInicioMillis)
        assertEquals("remoto-7", (result as ResultadoObra.Exito).dato.id)
    }

    @Test
    fun permisosYRedSeConviertenAErroresDeDominio() = runTest {
        val source = ObrasRemoteFalso()
        val repo = RepositorioObrasImpl(source)

        source.createResult = FirebaseObraResult.Failure(FirebaseObraFailure.PermissionDenied)
        assertEquals(
            ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA),
            repo.crear("uid", ObraDraft("N", "C", "D", false, Instant.fromEpochMilliseconds(0)), EstadoObra.ACTIVA),
        )
        source.listResult = FirebaseObraResult.Failure(FirebaseObraFailure.Network)
        assertEquals(ResultadoObra.Fallo(ErrorObra.RED), repo.observarPropias("uid").first())
    }

    @Test
    fun actualizarMapeaCamposSinIncluirUidNiEstado() = runTest {
        val source = ObrasRemoteFalso()
        val fecha = Instant.parse("2026-11-01T00:00:00Z")

        val result = RepositorioObrasImpl(source).actualizar(
            "uid-2", "obra-8", com.unaj.obratrack.domain.model.ObraDraft("Casa", "Cliente", "Calle", true, fecha),
        )

        assertEquals(ResultadoObra.Exito(Unit), result)
        assertEquals("uid-2", source.updatedUserId)
        assertEquals("obra-8", source.updatedObraId)
        assertEquals(
            ObraUpdateFields("Casa", "Cliente", "Calle", true, fecha.toEpochMilliseconds()),
            source.updatedFields,
        )
    }

    @Test
    fun estadoSeCodificaEnMinusculaYPermisosSeTipan() = runTest {
        val source = ObrasRemoteFalso()
        val repo = RepositorioObrasImpl(source)

        assertEquals(ResultadoObra.Exito(Unit), repo.cambiarEstado("uid-1", "obra-1", EstadoObra.FINALIZADA))
        assertEquals("finalizada", source.updatedState)

        source.updateResult = FirebaseObraResult.Failure(FirebaseObraFailure.PermissionDenied)
        assertEquals(
            ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA),
            repo.actualizar(
                "uid-1", "obra-1",
                com.unaj.obratrack.domain.model.ObraDraft("N", "C", "D", false, Instant.fromEpochMilliseconds(0)),
            ),
        )
    }

    private fun dto(id: String = "obra-1") = ObraDto(
        id = id, userId = "uid-1", nombre = "Escuela", cliente = "Municipio", direccion = "Calle 1",
        publica = false, estado = "activa", fechaInicioMillis = Instant.parse("2026-11-01T00:00:00Z").toEpochMilliseconds(),
    )
}

private class ObrasRemoteFalso : FirebaseObraDataSource {
    var queriedUserId: String? = null
    var createdDto: ObraDto? = null
    var listResult: FirebaseObraResult<List<ObraDto>> = FirebaseObraResult.Success(emptyList())
    var createResult: FirebaseObraResult<ObraDto> = FirebaseObraResult.Failure(FirebaseObraFailure.Unexpected)
    var updatedUserId: String? = null
    var updatedObraId: String? = null
    var updatedFields: ObraUpdateFields? = null
    var updatedState: String? = null
    var updateResult: FirebaseObraResult<Unit> = FirebaseObraResult.Success(Unit)

    override fun observarPropias(userId: String): Flow<FirebaseObraResult<List<ObraDto>>> {
        queriedUserId = userId
        return flowOf(listResult)
    }

    override suspend fun crear(dto: ObraDto): FirebaseObraResult<ObraDto> {
        createdDto = dto
        return createResult
    }

    override suspend fun actualizar(userId: String, obraId: String, fields: ObraUpdateFields): FirebaseObraResult<Unit> {
        updatedUserId = userId
        updatedObraId = obraId
        updatedFields = fields
        return updateResult
    }

    override suspend fun cambiarEstado(userId: String, obraId: String, estado: String): FirebaseObraResult<Unit> {
        updatedUserId = userId
        updatedObraId = obraId
        updatedState = estado
        return updateResult
    }
}
