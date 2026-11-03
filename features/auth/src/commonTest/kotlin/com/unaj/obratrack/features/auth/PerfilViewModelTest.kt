package com.unaj.obratrack.features.auth

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.ResumenObras
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {
    private fun conViewModel(
        repo: ObrasRepoFalso,
        sesion: SesionFalsa = SesionFalsa(),
        bloque: suspend TestScope.(PerfilViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = PerfilViewModel(ObservarSesion(sesion), ObservarObras(repo))
        try {
            // La pantalla suscripta mantiene viva la consulta, como en la app. Se lanza sin demora
            // para que el estado ya esté actualizado cuando el test lo lee.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
            bloque(viewModel)
        } finally {
            // Sin esto, la espera de WhileSubscribed se dispara después de resetMain y falla.
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mientrasLlegaLaLecturaEstaCargando() = conViewModel(ObrasRepoFalso(sinEmitir = true)) { viewModel ->
        assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)
    }

    @Test
    fun conObrasDesglosaPorEstadoYConsultaConElUidDeLaSesion() {
        val repo = ObrasRepoFalso(
            ResultadoObra.Exito(
                listOf(
                    obra("1", EstadoObra.ACTIVA),
                    obra("2", EstadoObra.ACTIVA),
                    obra("3", EstadoObra.PAUSADA),
                    obra("4", EstadoObra.FINALIZADA),
                ),
            ),
        )
        conViewModel(repo) { viewModel ->
            assertEquals(
                ResumenPerfilUiState.Contenido(ResumenObras(total = 4, activas = 2, pausadas = 1, finalizadas = 1)),
                viewModel.state.value,
            )
            assertEquals(listOf("uid-1"), repo.usuariosConsultados)
        }
    }

    @Test
    fun sinObrasEsContenidoEnCeroYNoUnError() = conViewModel(ObrasRepoFalso(ResultadoObra.Exito(emptyList()))) { viewModel ->
        assertEquals(
            ResumenPerfilUiState.Contenido(ResumenObras(total = 0, activas = 0, pausadas = 0, finalizadas = 0)),
            viewModel.state.value,
        )
    }

    @Test
    fun siFallaLaLecturaMuestraErrorYReintentarVuelveAConsultar() {
        val repo = ObrasRepoFalso(
            ResultadoObra.Fallo(ErrorObra.RED),
            ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA))),
        )
        conViewModel(repo) { viewModel ->
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.RED), viewModel.state.value)

            viewModel.reintentar()

            assertEquals(
                ResumenPerfilUiState.Contenido(ResumenObras(total = 1, activas = 1, pausadas = 0, finalizadas = 0)),
                viewModel.state.value,
            )
            assertEquals(2, repo.usuariosConsultados.size)
        }
    }

    @Test
    fun permisosYErroresDesconocidosSeTraducenAErroresDeUi() {
        val repo = ObrasRepoFalso(
            ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA),
            ResultadoObra.Fallo(ErrorObra.FALLO_PARCIAL),
        )
        conViewModel(repo) { viewModel ->
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.PERMISOS), viewModel.state.value)
            viewModel.reintentar()
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.DESCONOCIDO), viewModel.state.value)
        }
    }

    @Test
    fun sinSesionMuestraErrorDeSesionSinConsultarObras() {
        val repo = ObrasRepoFalso(ResultadoObra.Exito(emptyList()))
        conViewModel(repo, SesionFalsa(usuario = null)) { viewModel ->
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.SESION), viewModel.state.value)
            assertEquals(emptyList(), repo.usuariosConsultados)
        }
    }
}

private fun obra(id: String, estado: EstadoObra) = Obra(
    id = id,
    userId = "uid-1",
    nombre = "Obra $id",
    cliente = "Cliente",
    direccion = "Calle 1",
    publica = false,
    estado = estado,
    fechaInicio = Instant.parse("2026-11-01T00:00:00Z"),
)

private class SesionFalsa(
    usuario: Usuario? = Usuario("uid-1", "fede@example.com", null),
) : RepositorioAutenticacion {
    override val usuarioActual = MutableStateFlow(usuario)
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
}

/** Devuelve un resultado por consulta, en orden; el último se repite. Con [sinEmitir] nunca responde. */
private class ObrasRepoFalso(
    vararg resultados: ResultadoObra<List<Obra>>,
    private val sinEmitir: Boolean = false,
) : RepositorioObras {
    private val respuestas = resultados.toList()
    val usuariosConsultados = mutableListOf<String>()

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> {
        usuariosConsultados += userId
        if (sinEmitir) return MutableSharedFlow()
        return flowOf(respuestas[minOf(usuariosConsultados.size - 1, respuestas.lastIndex)])
    }

    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) = ResultadoObra.Exito(Unit)
    override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
}
