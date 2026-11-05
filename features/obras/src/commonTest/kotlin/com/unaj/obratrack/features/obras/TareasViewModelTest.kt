package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.ResumenTareas
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.usecase.ActualizarTarea
import com.unaj.obratrack.domain.usecase.CambiarEstadoTarea
import com.unaj.obratrack.domain.usecase.CrearTarea
import com.unaj.obratrack.domain.usecase.EliminarTarea
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.ObservarTareas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TareasViewModelTest {
    private fun conViewModel(
        repo: TareasRepoFalso = TareasRepoFalso(),
        sesion: SesionFalsa = SesionFalsa(),
        bloque: suspend kotlinx.coroutines.test.TestScope.(TareasViewModel, TareasRepoFalso) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = TareasViewModel(
                ObservarSesion(sesion), ObservarTareas(repo), CrearTarea(repo), ActualizarTarea(repo),
                CambiarEstadoTarea(repo), EliminarTarea(repo), RelojFijo,
            )
            advanceUntilIdle()
            bloque(viewModel, repo)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun observaLasTareasDeLaObraAbiertaConResumenYAtrasadas() = conViewModel(
        TareasRepoFalso().apply {
            tareas = listOf(
                tarea("1", "Cimientos", fin = LocalDate(2026, 11, 1)),
                tarea("2", "Pintura", EstadoTarea.COMPLETADA, fin = LocalDate(2026, 11, 1)),
            )
        },
    ) { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()

        val lista = assertIs<ListaTareasUiState.Contenido>(viewModel.state.value.lista)
        assertEquals(listOf("uid-1" to "obra-1"), repo.consultas)
        assertEquals(ResumenTareas(completadas = 1, pendientes = 1, atrasadas = 1), lista.resumen)
        assertEquals(listOf(true, false), lista.items.map { it.atrasada })
    }

    @Test
    fun sinTareasMuestraListaVaciaYAlCerrarLaObraSeLimpia() = conViewModel { viewModel, _ ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        assertEquals(ListaTareasUiState.Vacia, viewModel.state.value.lista)

        viewModel.abrirFormulario()
        viewModel.cerrarObra()
        assertEquals(TareasUiState(), viewModel.state.value)
    }

    @Test
    fun creaTareaConFechasIsoYCierraElFormulario() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        viewModel.abrirFormulario()
        viewModel.tituloCambiado("  Colar losa ")
        viewModel.fechaInicioCambiada("2026-11-10")
        viewModel.fechaFinCambiada("2026-11-12")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals("Colar losa", repo.creada?.titulo)
        assertEquals(LocalDate(2026, 11, 10), repo.creada?.fechaInicio)
        assertEquals(LocalDate(2026, 11, 12), repo.creada?.fechaFin)
        assertFalse(viewModel.state.value.formularioVisible)
    }

    @Test
    fun fechaConFormatoInvalidoNoLlamaAlRepositorio() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        viewModel.abrirFormulario()
        viewModel.tituloCambiado("Colar losa")
        viewModel.fechaInicioCambiada("10/11/2026")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(ErrorTareasUi.FECHA_FORMATO, viewModel.state.value.formulario.error)
        assertNull(repo.creada)
    }

    @Test
    fun erroresDeDominioQuedanTipadosEnElFormulario() = conViewModel { viewModel, _ ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        viewModel.abrirFormulario()
        viewModel.guardar()
        advanceUntilIdle()
        assertEquals(ErrorTareasUi.TITULO, viewModel.state.value.formulario.error)

        viewModel.tituloCambiado("T")
        viewModel.fechaInicioCambiada("2026-11-10")
        viewModel.guardar()
        advanceUntilIdle()
        assertEquals(ErrorTareasUi.FECHAS_INCOMPLETAS, viewModel.state.value.formulario.error)
        assertFalse(viewModel.state.value.formulario.guardando)
    }

    @Test
    fun editarCargaLaTareaYActualizaConSuId() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        viewModel.editar(tarea("9", "Pintar", EstadoTarea.EN_CURSO))
        viewModel.tituloCambiado("Pintar muros")

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals("9", repo.actualizadaId)
        assertEquals("Pintar muros", repo.actualizadaDraft?.titulo)
        assertEquals(EstadoTarea.EN_CURSO, repo.actualizadaDraft?.estado)
    }

    @Test
    fun cambiaElEstadoYInformaFallos() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        val tarea = tarea("9", "Pintar")

        viewModel.cambiarEstado(tarea, EstadoTarea.COMPLETADA)
        advanceUntilIdle()
        assertEquals(EstadoTarea.COMPLETADA, repo.estadoEnviado)
        assertNull(viewModel.state.value.errorOperacion)
        assertNull(viewModel.state.value.actualizandoId)

        repo.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.RED)
        viewModel.cambiarEstado(tarea, EstadoTarea.EN_CURSO)
        advanceUntilIdle()
        assertEquals(ErrorTareasUi.RED, viewModel.state.value.errorOperacion)
    }

    @Test
    fun eliminarPideConfirmacionYRespetaLaEleccionSobreRelevamientos() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        val tarea = tarea("9", "Pintar")

        viewModel.solicitarEliminacion(tarea)
        assertEquals(RelevamientosVinculados.DESVINCULAR, viewModel.state.value.vinculados)
        viewModel.cancelarEliminacion()
        advanceUntilIdle()
        assertNull(repo.eliminadaId)

        viewModel.solicitarEliminacion(tarea)
        viewModel.vinculadosCambiados(RelevamientosVinculados.ELIMINAR)
        viewModel.confirmarEliminacion()
        advanceUntilIdle()

        assertEquals("9", repo.eliminadaId)
        assertEquals(RelevamientosVinculados.ELIMINAR, repo.vinculadosEnviados)
        assertNull(viewModel.state.value.tareaAEliminar)
        assertFalse(viewModel.state.value.eliminando)
    }

    @Test
    fun bajaParcialSeInformaComoError() = conViewModel { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()
        repo.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.FALLO_PARCIAL)

        viewModel.solicitarEliminacion(tarea("9", "Pintar"))
        viewModel.confirmarEliminacion()
        advanceUntilIdle()

        assertEquals(ErrorTareasUi.BAJA_PARCIAL, viewModel.state.value.errorOperacion)
    }

    @Test
    fun guardarDemoradoNoCierraElFormularioDeOtraObra() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        val espera = CompletableDeferred<Unit>()
        repo.antesDeEscribir = { esperarSinCancelar(espera) }
        vm.abrirFormulario()
        vm.tituloCambiado("Primera")
        vm.guardar()
        advanceUntilIdle()
        vm.abrirObra("obra-2")
        advanceUntilIdle()
        vm.abrirFormulario()
        vm.tituloCambiado("Segunda")
        val nuevo = vm.state.value
        espera.complete(Unit)
        advanceUntilIdle()
        assertEquals(nuevo, vm.state.value)
    }

    @Test
    fun errorDeEdicionNoApareceAlCerrarYReabrirLaMismaObra() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        val espera = CompletableDeferred<Unit>()
        repo.antesDeEscribir = { esperarSinCancelar(espera) }
        repo.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.RED)
        vm.editar(tarea("1", "Anterior"))
        vm.guardar()
        advanceUntilIdle()
        vm.cerrarObra()
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        vm.abrirFormulario()
        vm.tituloCambiado("Nuevo")
        val nuevo = vm.state.value
        espera.complete(Unit)
        advanceUntilIdle()
        assertEquals(nuevo, vm.state.value)
    }

    @Test
    fun estadoAnteriorNoApagaLaOperacionDeLaNuevaObra() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        val anterior = CompletableDeferred<Unit>()
        val nueva = CompletableDeferred<Unit>()
        repo.antesDeEscribir = { esperarSinCancelar(anterior) }
        vm.cambiarEstado(tarea("1", "Anterior"), EstadoTarea.COMPLETADA)
        advanceUntilIdle()
        vm.abrirObra("obra-2")
        advanceUntilIdle()
        repo.antesDeEscribir = { esperarSinCancelar(nueva) }
        vm.cambiarEstado(tarea("2", "Nueva").copy(obraId = "obra-2"), EstadoTarea.COMPLETADA)
        advanceUntilIdle()
        val actual = vm.state.value
        anterior.complete(Unit)
        advanceUntilIdle()
        assertEquals(actual, vm.state.value)
        nueva.complete(Unit)
        advanceUntilIdle()
        assertNull(vm.state.value.actualizandoId)
    }

    @Test
    fun bajaAnteriorNoContaminaLaSesionNueva() {
        val sesion = SesionFalsa()
        conViewModel(sesion = sesion) { vm, repo ->
            vm.abrirObra("obra-1")
            advanceUntilIdle()
            val espera = CompletableDeferred<Unit>()
            repo.antesDeEscribir = { esperarSinCancelar(espera) }
            repo.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.RED)
            vm.solicitarEliminacion(tarea("1", "Anterior"))
            vm.confirmarEliminacion()
            advanceUntilIdle()
            sesion.usuarioActual.value = null
            advanceUntilIdle()
            sesion.usuarioActual.value = Usuario("uid-2", "otra@example.com", null)
            advanceUntilIdle()
            val actual = vm.state.value
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(actual, vm.state.value)
        }
    }

    @Test
    fun guardarAnteriorNoCierraOtroFormularioEnLaMismaObra() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        val espera = CompletableDeferred<Unit>()
        repo.antesDeEscribir = { esperarSinCancelar(espera) }
        vm.abrirFormulario()
        vm.tituloCambiado("Anterior")
        vm.guardar()
        advanceUntilIdle()
        vm.cerrarFormulario()
        vm.abrirFormulario()
        vm.tituloCambiado("Nuevo")
        val nuevo = vm.state.value
        espera.complete(Unit)
        advanceUntilIdle()
        assertEquals(nuevo, vm.state.value)
    }

    @Test
    fun excepcionDeBajaAntiguaNoApagaLaBajaActual() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        val anterior = CompletableDeferred<Unit>()
        val actual = CompletableDeferred<Unit>()
        repo.antesDeEscribir = {
            esperarSinCancelar(anterior)
            throw IllegalStateException("fallo anterior")
        }
        vm.solicitarEliminacion(tarea("1", "Anterior"))
        vm.confirmarEliminacion()
        advanceUntilIdle()
        vm.cerrarObra()
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        repo.antesDeEscribir = { esperarSinCancelar(actual) }
        vm.solicitarEliminacion(tarea("2", "Actual"))
        vm.confirmarEliminacion()
        advanceUntilIdle()
        val nuevo = vm.state.value
        anterior.complete(Unit)
        advanceUntilIdle()
        assertEquals(nuevo, vm.state.value)
        actual.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.eliminando)
    }

    @Test
    fun limiteDeBajaSeInformaSinDejarLaPantallaCargando() = conViewModel { vm, repo ->
        vm.abrirObra("obra-1")
        advanceUntilIdle()
        repo.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.DEMASIADOS_VINCULADOS)
        vm.solicitarEliminacion(tarea("1", "Tarea"))
        vm.confirmarEliminacion()
        advanceUntilIdle()
        assertEquals(ErrorTareasUi.DEMASIADOS_VINCULADOS, vm.state.value.errorOperacion)
        assertFalse(vm.state.value.eliminando)
    }

    @Test
    fun sinSesionNoConsultaYMuestraErrorDeSesion() = conViewModel(sesion = SesionFalsa(null)) { viewModel, repo ->
        viewModel.abrirObra("obra-1")
        advanceUntilIdle()

        assertEquals(ListaTareasUiState.Error(ErrorTareasUi.SESION), viewModel.state.value.lista)
        assertEquals(emptyList(), repo.consultas)
    }
}

private fun tarea(
    id: String,
    titulo: String,
    estado: EstadoTarea = EstadoTarea.PENDIENTE,
    fin: LocalDate? = null,
) = Tarea(
    id = id,
    obraId = "obra-1",
    userId = "uid-1",
    titulo = titulo,
    fechaInicio = fin,
    fechaFin = fin,
    estado = estado,
)

private object RelojFijo : Clock {
    override fun now(): Instant = Instant.parse("2026-11-02T15:00:00Z")
}

private class SesionFalsa(usuario: Usuario? = Usuario("uid-1", "fede@example.com", null)) : RepositorioAutenticacion {
    override val usuarioActual = MutableStateFlow(usuario)
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
}

private class TareasRepoFalso : RepositorioTareas {
    var antesDeEscribir: suspend () -> Unit = {}
    var tareas: List<Tarea> = emptyList()
    var resultadoUnit: ResultadoTarea<Unit> = ResultadoTarea.Exito(Unit)
    val consultas = mutableListOf<Pair<String, String>>()
    var creada: TareaDraft? = null
    var actualizadaId: String? = null
    var actualizadaDraft: TareaDraft? = null
    var estadoEnviado: EstadoTarea? = null
    var eliminadaId: String? = null
    var vinculadosEnviados: RelevamientosVinculados? = null

    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> {
        consultas += userId to obraId
        return flowOf(ResultadoTarea.Exito(tareas))
    }

    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> {
        antesDeEscribir()
        creada = draft
        return ResultadoTarea.Exito(tarea("nueva", draft.titulo))
    }

    override suspend fun actualizar(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft,
    ): ResultadoTarea<Unit> {
        antesDeEscribir()
        actualizadaId = tareaId
        actualizadaDraft = draft
        return resultadoUnit
    }

    override suspend fun cambiarEstado(
        userId: String,
        obraId: String,
        tareaId: String,
        estado: EstadoTarea,
    ): ResultadoTarea<Unit> {
        antesDeEscribir()
        estadoEnviado = estado
        return resultadoUnit
    }

    override suspend fun eliminar(
        userId: String,
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados,
    ): ResultadoTarea<Unit> {
        antesDeEscribir()
        eliminadaId = tareaId
        vinculadosEnviados = vinculados
        return resultadoUnit
    }
}

/** Simula un servicio que entrega su respuesta aunque se haya cancelado la espera. */
private suspend fun esperarSinCancelar(espera: CompletableDeferred<Unit>) {
    try {
        withContext(NonCancellable) { espera.await() }
    } catch (_: CancellationException) {
        // El resultado externo ya llegó; el ViewModel debe comprobar a qué pantalla pertenece.
    }
}
