package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.usecase.CrearObra
import com.unaj.obratrack.domain.usecase.ActualizarObra
import com.unaj.obratrack.domain.usecase.CambiarEstadoObra
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObrasViewModelTest {
    @Test
    fun creaObraConUsuarioDeSesionYDeshabilitaFormularioAlCompletar() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val session = AuthRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(session), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo),
            )
            advanceUntilIdle()

            viewModel.abrirFormulario()
            viewModel.nombreCambiado("Escuela")
            viewModel.clienteCambiado("Municipio")
            viewModel.direccionCambiada("Calle 1")
            viewModel.fechaCambiada("2026-11-01")
            viewModel.visibilidadCambiada(true)
            viewModel.guardar()
            advanceUntilIdle()

            assertEquals("uid-1", repo.createdUserId)
            assertEquals("Escuela", repo.createdDraft?.nombre)
            assertEquals(true, repo.createdDraft?.publica)
            assertEquals(EstadoObra.ACTIVA, repo.createdStatus)
            assertFalse(viewModel.state.value.formularioVisible)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cambiaLaConsultaAlCambiarLaSesionYDescartaElDetalleAnterior() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val obraA = obra("obra-a", "uid-1", "Obra de A")
            val obraB = obra("obra-b", "uid-2", "Obra de B")
            repo.obrasPorUsuario["uid-1"] = listOf(obraA)
            repo.obrasPorUsuario["uid-2"] = listOf(obraB)
            val session = AuthRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(session), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo),
            )
            advanceUntilIdle()

            viewModel.abrirDetalle(obraA)
            session.usuarioActual.value = Usuario("uid-2", "otra@example.com", null)
            advanceUntilIdle()

            assertEquals(listOf("uid-1", "uid-2"), repo.usuariosConsultados)
            assertNull(viewModel.state.value.obraSeleccionada)
            assertEquals(obraB, (viewModel.state.value.lista as ListaObrasUiState.Contenido).obras.single())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun guardaCambiosDeLaObraSeleccionadaConSuUid() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo),
            )
            advanceUntilIdle()
            viewModel.abrirDetalle(obra("obra-1", "uid-1", "Nombre anterior"))
            viewModel.editarSeleccionada()
            viewModel.nombreCambiado("Nombre nuevo")
            viewModel.guardar()
            advanceUntilIdle()

            assertEquals("uid-1", repo.updatedUserId)
            assertEquals("obra-1", repo.updatedObraId)
            assertEquals("Nombre nuevo", repo.updatedDraft?.nombre)
            assertFalse(viewModel.state.value.formularioVisible)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cambiaElEstadoDeLaObraSeleccionadaConSuUid() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo),
            )
            advanceUntilIdle()
            viewModel.abrirDetalle(obra("obra-1", "uid-1", "Casa"))
            viewModel.cambiarEstado(EstadoObra.PAUSADA)
            advanceUntilIdle()

            assertEquals("uid-1", repo.updatedUserId)
            assertEquals("obra-1", repo.updatedObraId)
            assertEquals(EstadoObra.PAUSADA, repo.updatedState)
            assertFalse(viewModel.state.value.actualizando)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun obra(id: String, userId: String, nombre: String) = Obra(
        id = id,
        userId = userId,
        nombre = nombre,
        cliente = "Cliente",
        direccion = "Calle 1",
        publica = false,
        estado = EstadoObra.ACTIVA,
        fechaInicio = kotlin.time.Instant.fromEpochMilliseconds(0),
    )
}

private class AuthRepoFalso : RepositorioAutenticacion {
    override val usuarioActual = MutableStateFlow<Usuario?>(Usuario("uid-1", "fede@example.com", null))
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
}

private class ObrasRepoFalso : RepositorioObras {
    var createdUserId: String? = null
    var createdDraft: ObraDraft? = null
    var createdStatus: EstadoObra? = null
    val usuariosConsultados = mutableListOf<String>()
    val obrasPorUsuario = mutableMapOf<String, List<Obra>>()
    var updatedUserId: String? = null
    var updatedObraId: String? = null
    var updatedDraft: ObraDraft? = null
    var updatedState: EstadoObra? = null
    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> {
        usuariosConsultados += userId
        return flowOf(ResultadoObra.Exito(obrasPorUsuario[userId].orEmpty()))
    }
    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra): ResultadoObra<Obra> {
        createdUserId = userId
        createdDraft = draft
        createdStatus = estado
        return ResultadoObra.Exito(Obra("remote-id", userId, draft.nombre, draft.cliente, draft.direccion, draft.publica, estado, draft.fechaInicio))
    }

    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft): ResultadoObra<Unit> {
        updatedUserId = userId
        updatedObraId = obraId
        updatedDraft = draft
        return ResultadoObra.Exito(Unit)
    }

    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra): ResultadoObra<Unit> {
        updatedUserId = userId
        updatedObraId = obraId
        updatedState = estado
        return ResultadoObra.Exito(Unit)
    }
}
