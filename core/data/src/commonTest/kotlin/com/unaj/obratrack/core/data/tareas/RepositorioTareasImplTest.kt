package com.unaj.obratrack.core.data.tareas

import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaDataSource
import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaFailure
import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaResult
import com.unaj.obratrack.core.firebase.tareas.TareaDto
import com.unaj.obratrack.core.firebase.tareas.TareaUpdateFields
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioTareasImplTest {
    private val remote = TareasRemoteFalso()
    private val repositorio = RepositorioTareasImpl(remote)

    @Test
    fun mapeaDocumentoConFechasIsoYEnumsPorNombre() = runTest {
        remote.lista = FirebaseTareaResult.Success(
            listOf(
                dto(categoria = "PLOMERIA", estado = "EN_CURSO", fechaInicio = "2026-10-21", fechaFin = "2026-10-25"),
            ),
        )

        val resultado = repositorio.observarDeObra("uid-1", "obra-1").first()

        assertEquals("uid-1", remote.consultaUserId)
        assertEquals("obra-1", remote.consultaObraId)
        assertEquals(
            ResultadoTarea.Exito(
                listOf(
                    Tarea(
                        id = "t-1",
                        obraId = "obra-1",
                        userId = "uid-1",
                        titulo = "Cañerías",
                        categoria = CategoriaTarea.PLOMERIA,
                        fechaInicio = LocalDate(2026, 10, 21),
                        fechaFin = LocalDate(2026, 10, 25),
                        estado = EstadoTarea.EN_CURSO,
                    ),
                ),
            ),
            resultado,
        )
    }

    @Test
    fun categoriaDesconocidaSeLeeComoOtrosYFechasAusentesComoNull() = runTest {
        remote.lista = FirebaseTareaResult.Success(listOf(dto(categoria = "NUEVA", fechaInicio = null, fechaFin = null)))

        val tarea = (repositorio.observarDeObra("uid-1", "obra-1").first() as ResultadoTarea.Exito).dato.single()

        assertEquals(CategoriaTarea.OTROS, tarea.categoria)
        assertEquals(null, tarea.fechaInicio)
        assertEquals(null, tarea.fechaFin)
    }

    @Test
    fun estadoDesconocidoSeInformaComoErrorYNoComoExito() = runTest {
        remote.lista = FirebaseTareaResult.Success(listOf(dto(estado = "ARCHIVADA")))

        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.DESCONOCIDO),
            repositorio.observarDeObra("uid-1", "obra-1").first(),
        )
    }

    @Test
    fun creaConUidObraFechasIsoYDevuelveIdRemoto() = runTest {
        remote.creada = FirebaseTareaResult.Success(dto(id = "remoto-7"))
        val draft = TareaDraft(
            titulo = "Pintar",
            categoria = CategoriaTarea.PINTURA,
            fechaInicio = LocalDate(2026, 11, 3),
            fechaFin = LocalDate(2026, 11, 9),
        )

        val resultado = repositorio.crear("uid-2", "obra-4", draft)

        assertEquals("uid-2", remote.dtoCreado?.userId)
        assertEquals("obra-4", remote.dtoCreado?.obraId)
        assertEquals("PINTURA", remote.dtoCreado?.categoria)
        assertEquals("PENDIENTE", remote.dtoCreado?.estado)
        assertEquals("2026-11-03", remote.dtoCreado?.fechaInicio)
        assertEquals("2026-11-09", remote.dtoCreado?.fechaFin)
        assertEquals("remoto-7", (resultado as ResultadoTarea.Exito).dato.id)
    }

    @Test
    fun actualizarEnviaCamposYFechasAusentesComoNull() = runTest {
        repositorio.actualizar("uid-1", "obra-1", "t-9", TareaDraft("Limpiar", estado = EstadoTarea.COMPLETADA))

        assertEquals("t-9", remote.tareaId)
        assertEquals(
            TareaUpdateFields("Limpiar", "", "OTROS", "COMPLETADA", null, null),
            remote.camposActualizados,
        )
    }

    @Test
    fun cambiarEstadoEnviaElNombreDelEnum() = runTest {
        repositorio.cambiarEstado("uid-1", "obra-1", "t-9", EstadoTarea.EN_CURSO)

        assertEquals("EN_CURSO", remote.estadoEnviado)
    }

    @Test
    fun eliminarTraduceLaEleccionSobreRelevamientos() = runTest {
        repositorio.eliminar("uid-1", "obra-1", "t-9", RelevamientosVinculados.DESVINCULAR)
        assertEquals(false, remote.eliminarRelevamientos)

        repositorio.eliminar("uid-1", "obra-1", "t-9", RelevamientosVinculados.ELIMINAR)
        assertEquals(true, remote.eliminarRelevamientos)
    }

    @Test
    fun fallosRemotosSeConviertenAErroresDeDominio() = runTest {
        remote.unit = FirebaseTareaResult.Failure(FirebaseTareaFailure.PermissionDenied)
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            repositorio.cambiarEstado("uid", "obra", "t", EstadoTarea.PENDIENTE),
        )

        remote.unit = FirebaseTareaResult.Failure(FirebaseTareaFailure.Partial)
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.FALLO_PARCIAL),
            repositorio.eliminar("uid", "obra", "t", RelevamientosVinculados.ELIMINAR),
        )

        remote.lista = FirebaseTareaResult.Failure(FirebaseTareaFailure.Network)
        assertEquals(ResultadoTarea.Fallo(ErrorTarea.RED), repositorio.observarDeObra("uid", "obra").first())
    }

    private fun dto(
        id: String = "t-1",
        categoria: String = "OTROS",
        estado: String = "PENDIENTE",
        fechaInicio: String? = null,
        fechaFin: String? = null,
    ) = TareaDto(
        id = id,
        obraId = "obra-1",
        userId = "uid-1",
        titulo = "Cañerías",
        categoria = categoria,
        estado = estado,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
    )
}

private class TareasRemoteFalso : FirebaseTareaDataSource {
    var lista: FirebaseTareaResult<List<TareaDto>> = FirebaseTareaResult.Success(emptyList())
    var creada: FirebaseTareaResult<TareaDto> = FirebaseTareaResult.Failure(FirebaseTareaFailure.Unexpected)
    var unit: FirebaseTareaResult<Unit> = FirebaseTareaResult.Success(Unit)
    var consultaUserId: String? = null
    var consultaObraId: String? = null
    var dtoCreado: TareaDto? = null
    var tareaId: String? = null
    var camposActualizados: TareaUpdateFields? = null
    var estadoEnviado: String? = null
    var eliminarRelevamientos: Boolean? = null

    override fun observarDeObra(userId: String, obraId: String): Flow<FirebaseTareaResult<List<TareaDto>>> {
        consultaUserId = userId
        consultaObraId = obraId
        return flowOf(lista)
    }

    override suspend fun crear(dto: TareaDto): FirebaseTareaResult<TareaDto> {
        dtoCreado = dto
        return creada
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        fields: TareaUpdateFields,
    ): FirebaseTareaResult<Unit> {
        this.tareaId = tareaId
        camposActualizados = fields
        return unit
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: String,
    ): FirebaseTareaResult<Unit> {
        estadoEnviado = estado
        return unit
    }

    override suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        eliminarRelevamientos: Boolean,
    ): FirebaseTareaResult<Unit> {
        this.eliminarRelevamientos = eliminarRelevamientos
        return unit
    }
}
