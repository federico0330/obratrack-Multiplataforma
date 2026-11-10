package com.unaj.obratrack.features.auth

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.ResumenObras
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.ErrorRegistro
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {
    private fun conViewModel(
        repo: ObrasRepoFalso,
        sesion: RepositorioAutenticacion = SesionFalsa(),
        bloque: suspend TestScope.(PerfilViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = PerfilViewModel(ObservarSesion(sesion), ObservarObras(repo))
        try {
            bloque(viewModel)
        } finally {
            // Sin esto, la espera de WhileSubscribed se dispara después de resetMain y falla.
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    /** Como la pantalla de perfil visible: mientras haya un suscriptor, la consulta sigue viva. */
    private fun TestScope.suscribir(viewModel: PerfilViewModel): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    @Test
    fun mientrasLlegaLaLecturaEstaCargando() = conViewModel(ObrasRepoFalso(pendiente())) { viewModel ->
        suscribir(viewModel)

        assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)
    }

    @Test
    fun conObrasDesglosaPorEstadoYConsultaConElUidDeLaSesion() {
        val repo = ObrasRepoFalso(
            respuesta(
                ResultadoObra.Exito(
                    listOf(
                        obra("1", EstadoObra.ACTIVA),
                        obra("2", EstadoObra.ACTIVA),
                        obra("3", EstadoObra.PAUSADA),
                        obra("4", EstadoObra.FINALIZADA),
                    ),
                ),
            ),
        )
        conViewModel(repo) { viewModel ->
            suscribir(viewModel)

            assertEquals(contenido(total = 4, activas = 2, pausadas = 1, finalizadas = 1), viewModel.state.value)
            assertEquals(listOf("uid-1"), repo.usuariosConsultados)
        }
    }

    @Test
    fun sinObrasEsContenidoEnCeroYNoUnError() =
        conViewModel(ObrasRepoFalso(respuesta(ResultadoObra.Exito(emptyList())))) { viewModel ->
            suscribir(viewModel)

            assertEquals(contenido(total = 0, activas = 0, pausadas = 0, finalizadas = 0), viewModel.state.value)
        }

    @Test
    fun errorLuegoReintentarMuestraCargandoYDespuesElContenido() {
        val segundaLectura = pendiente()
        val repo = ObrasRepoFalso(respuesta(ResultadoObra.Fallo(ErrorObra.RED)), segundaLectura)
        conViewModel(repo) { viewModel ->
            suscribir(viewModel)
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.RED), viewModel.state.value)

            viewModel.reintentar()
            assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)

            segundaLectura.tryEmit(ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA))))
            assertEquals(contenido(total = 1, activas = 1, pausadas = 0, finalizadas = 0), viewModel.state.value)
            assertEquals(2, repo.usuariosConsultados.size)
        }
    }

    @Test
    fun permisosYErroresDesconocidosSeTraducenAErroresDeUi() {
        val repo = ObrasRepoFalso(
            respuesta(ResultadoObra.Fallo(ErrorObra.NO_AUTORIZADA)),
            respuesta(ResultadoObra.Fallo(ErrorObra.FALLO_PARCIAL)),
        )
        conViewModel(repo) { viewModel ->
            suscribir(viewModel)
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.PERMISOS), viewModel.state.value)

            viewModel.reintentar()
            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.DESCONOCIDO), viewModel.state.value)
        }
    }

    @Test
    fun siFallaLaLecturaDeLaSesionMuestraErrorDeSesion() =
        conViewModel(ObrasRepoFalso(pendiente()), sesion = SesionQueFalla) { viewModel ->
            suscribir(viewModel)

            assertEquals(ResumenPerfilUiState.Error(ErrorResumenUi.SESION), viewModel.state.value)
        }

    @Test
    fun sinSesionNoConsultaObrasNiMuestraError() {
        val repo = ObrasRepoFalso(respuesta(ResultadoObra.Exito(emptyList())))
        conViewModel(repo, SesionFalsa(usuario = null)) { viewModel ->
            suscribir(viewModel)

            assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)
            assertEquals(emptyList(), repo.usuariosConsultados)
        }
    }

    @Test
    fun alVolverDespuesDeMasDeCincoSegundosMuestraElUltimoValorMientrasActualiza() {
        val segundaLectura = pendiente()
        val repo = ObrasRepoFalso(
            respuesta(ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA)))),
            segundaLectura,
        )
        conViewModel(repo) { viewModel ->
            val pestana = suscribir(viewModel)
            assertEquals(contenido(total = 1, activas = 1, pausadas = 0, finalizadas = 0), viewModel.state.value)

            // Se sale de la pestaña y pasa más que el tiempo de gracia: la consulta se detiene.
            pestana.cancel()
            advanceTimeBy(5_001)
            runCurrent()

            suscribir(viewModel)
            // La consulta nueva todavía no respondió: sigue el último valor, sin indicador de carga.
            assertEquals(contenido(total = 1, activas = 1, pausadas = 0, finalizadas = 0), viewModel.state.value)
            assertEquals(2, repo.usuariosConsultados.size)

            segundaLectura.tryEmit(
                ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA), obra("2", EstadoObra.PAUSADA))),
            )
            assertEquals(contenido(total = 2, activas = 1, pausadas = 1, finalizadas = 0), viewModel.state.value)
        }
    }

    @Test
    fun alCerrarSesionNoParpadeaUnErrorDeSesion() {
        val sesion = SesionFalsa()
        val repo = ObrasRepoFalso(respuesta(ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA)))))
        conViewModel(repo, sesion) { viewModel ->
            val vistos = mutableListOf<ResumenPerfilUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect { vistos += it } }
            assertEquals(contenido(total = 1, activas = 1, pausadas = 0, finalizadas = 0), viewModel.state.value)

            sesion.usuarioActual.value = null

            assertTrue(vistos.none { it is ResumenPerfilUiState.Error }, "No debe mostrarse ningún error: $vistos")
            assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)
        }
    }

    @Test
    fun otraCuentaNuncaVeElResumenDeLaAnterior() {
        val sesion = SesionFalsa()
        val lecturaDeLaSegundaCuenta = pendiente()
        val repo = ObrasRepoFalso(
            respuesta(ResultadoObra.Exito(listOf(obra("1", EstadoObra.ACTIVA), obra("2", EstadoObra.ACTIVA)))),
            lecturaDeLaSegundaCuenta,
        )
        conViewModel(repo, sesion) { viewModel ->
            suscribir(viewModel)
            assertEquals(contenido(total = 2, activas = 2, pausadas = 0, finalizadas = 0), viewModel.state.value)

            sesion.usuarioActual.value = null
            sesion.usuarioActual.value = Usuario("uid-2", "otra@example.com", null)

            assertEquals(ResumenPerfilUiState.Cargando, viewModel.state.value)
            assertEquals(listOf("uid-1", "uid-2"), repo.usuariosConsultados)

            lecturaDeLaSegundaCuenta.tryEmit(ResultadoObra.Exito(emptyList()))
            assertEquals(contenido(total = 0, activas = 0, pausadas = 0, finalizadas = 0), viewModel.state.value)
        }
    }
}

private fun contenido(total: Int, activas: Int, pausadas: Int, finalizadas: Int) =
    ResumenPerfilUiState.Contenido(ResumenObras(total, activas, pausadas, finalizadas))

private fun respuesta(resultado: ResultadoObra<List<Obra>>): Flow<ResultadoObra<List<Obra>>> = flowOf(resultado)

/** Lectura que no responde hasta que el test emite, para observar el estado mientras espera. */
private fun pendiente() = MutableSharedFlow<ResultadoObra<List<Obra>>>(replay = 1)

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

private object SesionQueFalla : RepositorioAutenticacion {
    override val usuarioActual: Flow<Usuario?> = flow { throw IllegalStateException("sesión no disponible") }
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Fallo(ErrorAuth.Inesperado)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Fallo(ErrorRegistro.Inesperado)
}

/** Devuelve una lectura por consulta, en orden; la última se repite. */
private class ObrasRepoFalso(vararg lecturas: Flow<ResultadoObra<List<Obra>>>) : RepositorioObras {
    private val respuestas = lecturas.toList()
    val usuariosConsultados = mutableListOf<String>()

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> {
        usuariosConsultados += userId
        return respuestas[minOf(usuariosConsultados.size - 1, respuestas.lastIndex)]
    }

    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) = ResultadoObra.Exito(Unit)
    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: kotlin.time.Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> = error("Operación no usada en esta prueba")

    override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
}
