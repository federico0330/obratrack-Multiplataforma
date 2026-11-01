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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObrasViewModelTest {
    @Test
    fun creaObraConUsuarioDeSesionYDeshabilitaFormularioAlCompletar() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val session = AuthRepoFalso()
            val viewModel = ObrasViewModel(ObservarSesion(session), ObservarObras(repo), CrearObra(repo))
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
    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> = flowOf(ResultadoObra.Exito(emptyList()))
    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra): ResultadoObra<Obra> {
        createdUserId = userId
        createdDraft = draft
        createdStatus = estado
        return ResultadoObra.Exito(Obra("remote-id", userId, draft.nombre, draft.cliente, draft.direccion, draft.publica, estado, draft.fechaInicio))
    }
}
