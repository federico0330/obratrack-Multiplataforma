package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModelStore
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.*
import com.unaj.obratrack.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RelevamientosViewModelTest {
    private fun conVm(
        sesion: SesionRelevamientosFalsa = SesionRelevamientosFalsa(),
        repo: RelevamientosVmFalso = RelevamientosVmFalso(),
        tareas: TareasRelevamientosFalso = TareasRelevamientosFalso(),
        bloque: suspend TestScope.(RelevamientosViewModel, RelevamientosVmFalso) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val vm = RelevamientosViewModel(ObservarSesion(sesion), ObservarRelevamientos(repo),
                CrearRelevamiento(repo), ObservarTareas(tareas), RelojRelevamientos)
            store.put("relevamientos", vm)
            advanceUntilIdle()
            vm.abrirObra("obra")
            advanceUntilIdle()
            bloque(vm, repo)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun iniciaConListaVaciaYTareasDeLaObraActual() = conVm { vm, repo ->
        assertEquals(ListaRelevamientosUiState.Vacia, vm.state.value.lista)
        assertEquals(listOf("uid" to "obra"), repo.consultas)
        assertEquals("tarea", vm.state.value.tareas.items.single().id)
        assertFalse(vm.state.value.tareas.cargando)
    }

    @Test
    fun guardaFechaUtcNotaNormalizadaYVinculoSinDuplicarLaAccion() = conVm { vm, repo ->
        vm.abrirFormulario()
        vm.fechaCambiada("2026-11-05")
        vm.notaCambiada("  Se revisó la instalación  ")
        vm.tareaCambiada("tarea")
        vm.guardar()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(1, repo.altas)
        assertEquals("Se revisó la instalación", repo.draft?.nota)
        assertEquals("tarea", repo.draft?.tareaId)
        assertEquals(Instant.parse("2026-11-05T00:00:00Z"), repo.draft?.fecha)
        assertFalse(vm.state.value.formularioVisible)
        assertIs<ListaRelevamientosUiState.Contenido>(vm.state.value.lista)
    }

    @Test
    fun validaNotaYFormatoDeFechaSinEscribir() = conVm { vm, repo ->
        vm.abrirFormulario()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(ErrorRelevamientosUi.NOTA, vm.state.value.formulario.error)
        vm.notaCambiada("Nota")
        vm.fechaCambiada("31/02/2026")
        vm.guardar()
        assertEquals(ErrorRelevamientosUi.FECHA, vm.state.value.formulario.error)
        assertEquals(0, repo.altas)
        assertFalse(vm.state.value.formulario.guardando)
    }

    @Test
    fun tareaEliminadaSeInformaSinPerderElBorrador() = conVm { vm, repo ->
        repo.falloAlta = ErrorRelevamiento.TAREA_NO_DISPONIBLE
        vm.abrirFormulario()
        vm.notaCambiada("Nota")
        vm.tareaCambiada("tarea")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(ErrorRelevamientosUi.TAREA, vm.state.value.formulario.error)
        assertEquals("Nota", vm.state.value.formulario.nota)
        assertTrue(vm.state.value.formularioVisible)
        assertFalse(vm.state.value.formulario.guardando)
    }

    @Test
    fun errorDeTareasPermiteGuardarSinVinculo() {
        val tareas = TareasRelevamientosFalso().apply { estado.value = ResultadoTarea.Fallo(ErrorTarea.RED) }
        conVm(tareas = tareas) { vm, repo ->
            assertTrue(vm.state.value.tareas.error)
            vm.abrirFormulario()
            vm.notaCambiada("Nota sin tarea")
            vm.guardar()
            advanceUntilIdle()
            assertNull(repo.draft?.tareaId)
            assertFalse(vm.state.value.formularioVisible)
        }
    }

    @Test
    fun falloDeRedConservaCamposYPermiteReintentar() = conVm { vm, repo ->
        repo.falloAlta = ErrorRelevamiento.RED
        vm.abrirFormulario()
        vm.notaCambiada("Nota")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(ErrorRelevamientosUi.RED, vm.state.value.formulario.error)
        repo.falloAlta = null
        vm.guardar()
        advanceUntilIdle()
        assertFalse(vm.state.value.formularioVisible)
    }

    @Test
    fun listaConErrorPuedeReintentarseSinCambiarDeObra() = conVm { vm, repo ->
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Fallo(ErrorRelevamiento.RED)
        advanceUntilIdle()
        assertEquals(ListaRelevamientosUiState.Error(ErrorRelevamientosUi.RED), vm.state.value.lista)
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(emptyList())
        vm.reintentar()
        advanceUntilIdle()
        assertEquals(ListaRelevamientosUiState.Vacia, vm.state.value.lista)
        assertEquals(2, repo.consultas.size)
    }

    @Test
    fun altaDemoradaNoCierraElFormularioDeOtraObraNiDeUnaReapertura() = conVm { vm, repo ->
        for (destino in listOf("otra", "obra")) {
            vm.abrirObra("obra")
            advanceUntilIdle()
            val espera = CompletableDeferred<Unit>()
            repo.espera = espera
            vm.abrirFormulario()
            vm.notaCambiada("Anterior")
            vm.guardar()
            advanceUntilIdle()
            vm.cerrarObra()
            vm.abrirObra(destino)
            advanceUntilIdle()
            vm.abrirFormulario()
            vm.notaCambiada("Nuevo")
            espera.complete(Unit)
            advanceUntilIdle()
            assertTrue(vm.state.value.formularioVisible)
            assertEquals("Nuevo", vm.state.value.formulario.nota)
            assertNull(vm.state.value.formulario.error)
        }
    }

    @Test
    fun cambioDeCuentaLimpiaElEstadoYDescartaElErrorAnterior() {
        val sesion = SesionRelevamientosFalsa()
        conVm(sesion = sesion) { vm, repo ->
            val espera = CompletableDeferred<Unit>()
            repo.espera = espera
            repo.falloAlta = ErrorRelevamiento.RED
            vm.abrirFormulario()
            vm.notaCambiada("Cuenta anterior")
            vm.guardar()
            advanceUntilIdle()
            sesion.usuarioActual.value = Usuario("otro", "otro@example.com", null)
            advanceUntilIdle()
            val nuevo = vm.state.value
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(nuevo, vm.state.value)
            assertEquals("otro" to "obra", repo.consultas.last())
            assertTrue(vm.state.value.tareas.items.isEmpty())
        }
    }

    @Test
    fun sinSesionNoConsultaNiGuarda() {
        conVm(sesion = SesionRelevamientosFalsa(null)) { vm, repo ->
            assertEquals(ListaRelevamientosUiState.Error(ErrorRelevamientosUi.SESION), vm.state.value.lista)
            vm.abrirFormulario()
            vm.guardar()
            assertEquals(ErrorRelevamientosUi.SESION, vm.state.value.formulario.error)
            assertTrue(repo.consultas.isEmpty())
            assertEquals(0, repo.altas)
        }
    }

    @Test
    fun noPermiteEditarElBorradorMientrasSeGuarda() = conVm { vm, repo ->
        val espera = CompletableDeferred<Unit>()
        repo.espera = espera
        vm.abrirFormulario()
        vm.notaCambiada("Nota enviada")
        vm.guardar()
        advanceUntilIdle()
        vm.notaCambiada("Otra")
        vm.cerrarFormulario()
        assertEquals("Nota enviada", vm.state.value.formulario.nota)
        assertTrue(vm.state.value.formularioVisible)
        espera.complete(Unit)
        advanceUntilIdle()
    }
}

private object RelojRelevamientos : Clock {
    override fun now() = Instant.parse("2026-11-05T12:00:00Z")
}
private class SesionRelevamientosFalsa(usuario: Usuario? = Usuario("uid", "fede@example.com", null)) : RepositorioAutenticacion {
    override val usuarioActual = MutableStateFlow(usuario)
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
}
private class RelevamientosVmFalso : RepositorioRelevamientos {
    val consultas = mutableListOf<Pair<String, String>>()
    private val streams = mutableMapOf<Pair<String, String>, MutableStateFlow<ResultadoRelevamiento<List<Relevamiento>>>>()
    var falloAlta: ErrorRelevamiento? = null
    var espera: CompletableDeferred<Unit>? = null
    var altas = 0
    var draft: RelevamientoDraft? = null
    fun stream(uid: String, obra: String) = streams.getOrPut(uid to obra) { MutableStateFlow(ResultadoRelevamiento.Exito(emptyList())) }
    override fun observarDeObra(userId: String, obraId: String) = stream(userId, obraId).also { consultas += userId to obraId }
    override suspend fun crear(userId: String, obraId: String, draft: RelevamientoDraft): ResultadoRelevamiento<Relevamiento> {
        altas++
        this.draft = draft
        espera?.let {
            try { withContext(NonCancellable) { it.await() } } catch (_: CancellationException) { }
        }
        falloAlta?.let { return ResultadoRelevamiento.Fallo(it) }
        val item = Relevamiento("r-$altas", obraId, userId, draft.fecha, draft.nota, draft.tareaId)
        stream(userId, obraId).value = ResultadoRelevamiento.Exito(listOf(item))
        return ResultadoRelevamiento.Exito(item)
    }
}
private class TareasRelevamientosFalso : RepositorioTareas {
    val estado = MutableStateFlow<ResultadoTarea<List<Tarea>>>(ResultadoTarea.Exito(listOf(Tarea("tarea", "obra", "uid", "Instalación"))))
    override fun observarDeObra(userId: String, obraId: String) = estado.map { result ->
        when (result) {
            is ResultadoTarea.Exito -> ResultadoTarea.Exito(result.dato.filter { it.userId == userId && it.obraId == obraId })
            is ResultadoTarea.Fallo -> result
        }
    }
    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> = error("No usado")
    override suspend fun actualizar(userId: String, obraId: String, tareaId: String, draft: TareaDraft): ResultadoTarea<Unit> = error("No usado")
    override suspend fun cambiarEstado(userId: String, obraId: String, tareaId: String, estado: EstadoTarea): ResultadoTarea<Unit> = error("No usado")
    override suspend fun eliminar(userId: String, obraId: String, tareaId: String, vinculados: RelevamientosVinculados): ResultadoTarea<Unit> = error("No usado")
}
