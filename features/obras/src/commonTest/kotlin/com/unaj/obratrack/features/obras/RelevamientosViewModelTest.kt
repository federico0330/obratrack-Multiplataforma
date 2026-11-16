package com.unaj.obratrack.features.obras

import androidx.lifecycle.ViewModelStore
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.CambioUbicacion
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.model.FiltroRelevamientos
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RelevamientosViewModelTest {
    private fun conVm(
        sesion: SesionRelevamientosFalsa = SesionRelevamientosFalsa(),
        repo: RelevamientosVmFalso = RelevamientosVmFalso(),
        tareas: TareasRelevamientosFalso = TareasRelevamientosFalso(),
        fotos: FotosVmFalso = FotosVmFalso(),
        ubicaciones: UbicacionVmFalsa = UbicacionVmFalsa(),
        direcciones: DireccionesVmFalsa = DireccionesVmFalsa(),
        bloque: suspend TestScope.(RelevamientosViewModel, RelevamientosVmFalso) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val vm = RelevamientosViewModel(ObservarSesion(sesion), ObservarRelevamientos(repo),
                CrearRelevamiento(repo, fotos), ObservarTareas(tareas), ActualizarRelevamiento(repo, fotos), EliminarRelevamiento(repo),
                ObtenerUbicacionActual(ubicaciones, 15.seconds), ObtenerDireccion(direcciones), BuscarDirecciones(direcciones), RelojRelevamientos)
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
    fun permiteSeguirEditandoYDetectaSiSeDeshizoElCambio() = conVm { vm, _ ->
        vm.abrirFormulario()
        vm.notaCambiada("Medidas de la abertura")
        vm.solicitarCerrarFormulario()
        assertTrue(vm.state.value.formulario.confirmarDescarte)

        vm.seguirEditando()
        assertFalse(vm.state.value.formulario.confirmarDescarte)
        assertEquals("Medidas de la abertura", vm.state.value.formulario.nota)

        vm.notaCambiada("")
        vm.solicitarCerrarFormulario()
        assertFalse(vm.state.value.formularioVisible)
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
        vm.solicitarCerrarFormulario()
        assertFalse(vm.state.value.formulario.confirmarDescarte)
        assertEquals("Nota enviada", vm.state.value.formulario.nota)
        assertTrue(vm.state.value.formularioVisible)
        espera.complete(Unit)
        advanceUntilIdle()
    }
    @Test
    fun abreDetalleEditaFechaNotaYDesvinculaSinPerderCamposAdicionales() = conVm { vm, repo ->
        val item = Relevamiento("original", "obra", "uid", Instant.parse("2026-11-05T00:00:00Z"), "Original", "tarea", fotoUrl = "https://example.com/foto")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        vm.abrirDetalle(item)
        vm.editarSeleccionado()
        assertEquals("original", vm.state.value.formulario.id)
        assertEquals("2026-11-05", vm.state.value.formulario.fechaIso)
        vm.notaCambiada(" Editada ")
        vm.fechaCambiada("2026-11-03")
        vm.tareaCambiada(null)
        vm.guardar()
        advanceUntilIdle()
        assertFalse(vm.state.value.formularioVisible)
        assertEquals("Editada", vm.state.value.seleccionado?.nota)
        assertNull(vm.state.value.seleccionado?.tareaId)
        assertEquals(item.fotoUrl, vm.state.value.seleccionado?.fotoUrl)
    }

    @Test
    fun cancelarLaBajaNoEscribeYLaListaPuedeActualizarAntesDeLaConfirmacion() = conVm { vm, repo ->
        val item = Relevamiento("id", "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        vm.abrirDetalle(item)
        vm.solicitarEliminacion()
        vm.cancelarEliminacion()
        assertNull(repo.eliminado)
        vm.solicitarEliminacion()
        val espera = CompletableDeferred<Unit>()
        repo.esperaCambio = espera
        vm.confirmarEliminacion()
        advanceUntilIdle()
        assertTrue(vm.state.value.eliminando)
        assertNull(vm.state.value.seleccionado)
        espera.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.eliminando)
        assertEquals(ListaRelevamientosUiState.Vacia, vm.state.value.lista)
        vm.abrirFormulario()
        assertTrue(vm.state.value.formularioVisible)
    }

    @Test
    fun bajaFallidaMuestraErrorYLiberaLosControles() = conVm { vm, repo ->
        val item = Relevamiento("id", "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        repo.falloCambio = ErrorRelevamiento.RED
        vm.abrirDetalle(item)
        vm.solicitarEliminacion()
        vm.confirmarEliminacion()
        advanceUntilIdle()
        assertEquals(ErrorRelevamientosUi.RED, vm.state.value.errorOperacion)
        assertFalse(vm.state.value.eliminando)
        assertEquals(item, vm.state.value.seleccionado)
    }

    @Test
    fun filtroNoReconsultaYUnRegistroEliminadoDesdeOtroClienteCierraElDetalle() = conVm { vm, repo ->
        val item = Relevamiento("id", "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota", "tarea")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        vm.filtroCambiado(FiltroRelevamientos.SinTarea)
        assertEquals(ListaRelevamientosUiState.Vacia, vm.state.value.lista)
        assertEquals(1, repo.consultas.size)
        vm.filtroCambiado(FiltroRelevamientos.Todos)
        vm.abrirDetalle(item)
        vm.editarSeleccionado()
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(emptyList())
        advanceUntilIdle()
        assertNull(vm.state.value.seleccionado)
        assertFalse(vm.state.value.formularioVisible)
        assertEquals(ErrorRelevamientosUi.INEXISTENTE, vm.state.value.errorOperacion)
    }

    @Test
    fun bajaDemoradaNoContaminaOtraObra() = conVm { vm, repo ->
        val item = Relevamiento("id", "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        repo.falloCambio = ErrorRelevamiento.RED
        val espera = CompletableDeferred<Unit>()
        repo.esperaCambio = espera
        vm.abrirDetalle(item)
        vm.solicitarEliminacion()
        vm.confirmarEliminacion()
        advanceUntilIdle()
        vm.cerrarObra()
        vm.abrirObra("otra")
        advanceUntilIdle()
        vm.abrirFormulario()
        val nuevo = vm.state.value
        espera.complete(Unit)
        advanceUntilIdle()
        assertEquals(nuevo, vm.state.value)
    }

    @Test
    fun borradoExternoDuranteEdicionPermiteCrearSinEsperarLaRespuestaVieja() = conVm { vm, repo ->
        val item = Relevamiento("id", "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota")
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        val espera = CompletableDeferred<Unit>()
        repo.esperaCambio = espera
        repo.falloCambio = ErrorRelevamiento.NO_ENCONTRADO
        vm.abrirDetalle(item)
        vm.editarSeleccionado()
        vm.guardar()
        advanceUntilIdle()
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(emptyList())
        advanceUntilIdle()
        vm.abrirFormulario()
        vm.notaCambiada("Nuevo borrador")
        val actual = vm.state.value
        assertTrue(actual.formularioVisible)
        espera.complete(Unit)
        advanceUntilIdle()
        assertEquals(actual, vm.state.value)
    }

    @Test
    fun cambiarFiltroMientrasCargaNoMuestraUnVacioFalso() = conVm { vm, _ ->
        vm.abrirObra("otra")
        vm.filtroCambiado(FiltroRelevamientos.SinTarea)
        assertEquals(ListaRelevamientosUiState.Cargando, vm.state.value.lista)
        advanceUntilIdle()
        assertEquals(ListaRelevamientosUiState.Vacia, vm.state.value.lista)
    }

    @Test
    fun siFallaLaSubidaConservaElFormularioYMuestraElErrorTipado() {
        val fotos = FotosVmFalso(ResultadoSubidaFoto.SinConexion)
        conVm(fotos = fotos) { vm, repo ->
            vm.abrirFormulario()
            vm.notaCambiada("Con foto")
            vm.fotoElegida(byteArrayOf(1, 2))
            val antes = vm.state.value.formulario
            vm.guardar()
            advanceUntilIdle()
            val form = vm.state.value.formulario
            assertEquals(ErrorRelevamientosUi.FOTO_SIN_CONEXION, form.error)
            assertEquals(antes.copy(error = ErrorRelevamientosUi.FOTO_SIN_CONEXION), form)
            assertTrue(vm.state.value.formularioVisible)
            assertEquals(1, fotos.subidas)
            assertEquals(0, repo.altas)
        }
    }

    @Test
    fun crearConFotoGuardaLaUrlSubida() {
        val fotos = FotosVmFalso()
        conVm(fotos = fotos) { vm, repo ->
            vm.abrirFormulario()
            vm.notaCambiada("Con foto")
            vm.fotoElegida(byteArrayOf(1))
            vm.guardar()
            advanceUntilIdle()
            assertEquals("https://foto", repo.fotoUrl)
            assertFalse(vm.state.value.formularioVisible)
        }
    }

    @Test
    fun editarConservaQuitaOReemplazaLaFotoExistente() {
        val fotos = FotosVmFalso()
        conVm(fotos = fotos) { vm, repo ->
            val item = Relevamiento("id", "obra", "uid", Instant.parse("2026-11-05T00:00:00Z"), "Nota", fotoUrl = "https://vieja",
                fotoVistaUrl = "https://vieja-reducida")
            repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
            advanceUntilIdle()
            fun editarYGuardar(cambio: RelevamientosViewModel.() -> Unit) {
                vm.abrirDetalle(item)
                vm.editarSeleccionado()
                vm.cambio()
                vm.guardar()
            }
            vm.abrirDetalle(item)
            vm.editarSeleccionado()
            assertEquals(FotoFormulario.Existente("https://vieja", "https://vieja-reducida"), vm.state.value.formulario.foto)
            vm.cerrarFormulario()
            editarYGuardar { }
            advanceUntilIdle()
            assertEquals(CambioFotoUrl.SinCambio, repo.cambioFoto)
            editarYGuardar { quitarFoto() }
            advanceUntilIdle()
            assertEquals(CambioFotoUrl.Quitar, repo.cambioFoto)
            editarYGuardar { fotoElegida(byteArrayOf(3)) }
            advanceUntilIdle()
            assertEquals(CambioFotoUrl.Nueva("https://foto"), repo.cambioFoto)
            assertEquals(1, fotos.subidas)
        }
    }

    @Test
    fun laSubidaDeOtraObraNoAfectaElFormularioNuevo() {
        val espera = CompletableDeferred<Unit>()
        val fotos = FotosVmFalso(espera = espera)
        conVm(fotos = fotos) { vm, repo ->
            vm.abrirFormulario()
            vm.notaCambiada("Con foto")
            vm.fotoElegida(byteArrayOf(1))
            vm.guardar()
            advanceUntilIdle()
            vm.cerrarObra()
            vm.abrirObra("otra")
            advanceUntilIdle()
            vm.abrirFormulario()
            vm.notaCambiada("Otra obra")
            val nuevo = vm.state.value
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(nuevo, vm.state.value)
        }
    }

    @Test
    fun fotoIlegibleMuestraErrorYUnaFotoTardiaConFormularioCerradoSeIgnora() = conVm { vm, _ ->
        vm.abrirFormulario()
        vm.fotoNoLeida()
        assertEquals(ErrorRelevamientosUi.FOTO_LECTURA, vm.state.value.formulario.error)
        vm.cerrarFormulario()
        val cerrado = vm.state.value
        vm.fotoElegida(byteArrayOf(1))
        assertEquals(cerrado, vm.state.value)
    }

    @Test
    fun camaraNoDisponibleMuestraErrorYConservaElFormulario() = conVm { vm, _ ->
        vm.abrirFormulario()
        vm.notaCambiada("Revoque")
        vm.fotoElegida(byteArrayOf(7))
        val antes = vm.state.value.formulario
        vm.camaraNoDisponible()
        assertEquals(antes.copy(error = ErrorRelevamientosUi.FOTO_CAMARA), vm.state.value.formulario)
        // Una foto elegida después limpia el error.
        vm.fotoElegida(byteArrayOf(8))
        assertEquals(null, vm.state.value.formulario.error)
    }

    private val casa = Ubicacion(-34.7746, -58.2672)
    private val obra = Ubicacion(-34.6, -58.4)

    private fun relevamientoCon(ubicacion: Ubicacion?) =
        Relevamiento("id", "obra", "uid", Instant.parse("2026-11-05T00:00:00Z"), "Nota", ubicacion = ubicacion)

    @Test
    fun capturaLaUbicacionYLaMuestraEnElFormulario() {
        val ubicaciones = UbicacionVmFalsa { ResultadoUbicacion.Exito(casa) }
        conVm(ubicaciones = ubicaciones) { vm, _ ->
            vm.abrirFormulario()
            vm.notaCambiada("Con ubicación")
            vm.capturarUbicacion()
            advanceUntilIdle()
            val form = vm.state.value.formulario
            assertEquals(casa, form.ubicacion)
            assertFalse(form.capturandoUbicacion)
            assertNull(form.error)
            assertEquals("Con ubicación", form.nota)
            assertEquals(1, ubicaciones.busquedas)
        }
    }

    @Test
    fun mientrasCapturaMuestraElIndicadorYNoSePuedeGuardarNiCapturarDeNuevo() {
        val espera = CompletableDeferred<Unit>()
        val ubicaciones = UbicacionVmFalsa(espera = espera)
        conVm(ubicaciones = ubicaciones) { vm, repo ->
            vm.abrirFormulario()
            vm.notaCambiada("Con ubicación")
            vm.capturarUbicacion()
            vm.capturarUbicacion()
            // runCurrent y no advanceUntilIdle: este avanzaría el reloj virtual hasta vencer el tiempo máximo.
            runCurrent()
            assertTrue(vm.state.value.formulario.capturandoUbicacion)
            vm.guardar()
            runCurrent()
            assertEquals(0, repo.altas)
            assertFalse(vm.state.value.formulario.guardando)
            assertEquals(1, ubicaciones.busquedas)
            espera.complete(Unit)
            runCurrent()
            assertFalse(vm.state.value.formulario.capturandoUbicacion)
            assertEquals(casa, vm.state.value.formulario.ubicacion)
            vm.guardar()
            advanceUntilIdle()
            assertEquals(1, repo.altas)
        }
    }

    @Test
    fun cadaFalloMuestraSuMensajeYConservaLaUbicacionPrevia() {
        val casos = listOf(
            ResultadoUbicacion.SinPermiso to ErrorRelevamientosUi.UBICACION_SIN_PERMISO,
            ResultadoUbicacion.Desactivada to ErrorRelevamientosUi.UBICACION_DESACTIVADA,
            ResultadoUbicacion.NoDisponible to ErrorRelevamientosUi.UBICACION_NO_DISPONIBLE,
        )
        for ((resultado, error) in casos) {
            conVm(ubicaciones = UbicacionVmFalsa { resultado }) { vm, repo ->
                val item = relevamientoCon(obra)
                repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
                advanceUntilIdle()
                vm.abrirDetalle(item)
                vm.editarSeleccionado()
                vm.capturarUbicacion()
                advanceUntilIdle()
                val form = vm.state.value.formulario
                assertEquals(error, form.error)
                assertEquals(obra, form.ubicacion)
                assertFalse(form.capturandoUbicacion)
            }
        }
    }

    @Test
    fun unaExcepcionInesperadaDelProveedorEsNoDisponible() {
        conVm(ubicaciones = UbicacionVmFalsa { throw IllegalStateException("sin proveedor") }) { vm, _ ->
            vm.abrirFormulario()
            vm.capturarUbicacion()
            advanceUntilIdle()
            assertEquals(ErrorRelevamientosUi.UBICACION_NO_DISPONIBLE, vm.state.value.formulario.error)
            assertNull(vm.state.value.formulario.ubicacion)
            assertFalse(vm.state.value.formulario.capturandoUbicacion)
        }
    }

    @Test
    fun cancelarCortaLaBusquedaSinErrorYConservaLaUbicacionPrevia() {
        val espera = CompletableDeferred<Unit>()
        conVm(ubicaciones = UbicacionVmFalsa(espera = espera)) { vm, repo ->
            val item = relevamientoCon(obra)
            repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
            advanceUntilIdle()
            vm.abrirDetalle(item)
            vm.editarSeleccionado()
            vm.capturarUbicacion()
            advanceUntilIdle()
            assertTrue(vm.state.value.formulario.capturandoUbicacion)
            vm.cancelarCaptura()
            val cancelado = vm.state.value
            assertFalse(cancelado.formulario.capturandoUbicacion)
            assertEquals(obra, cancelado.formulario.ubicacion)
            assertNull(cancelado.formulario.error)
            // Aunque la respuesta llegue igual, ya no cuenta.
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(cancelado, vm.state.value)
        }
    }

    @Test
    fun laRespuestaConElFormularioCerradoOReabiertoSeDescarta() {
        val espera = CompletableDeferred<Unit>()
        conVm(ubicaciones = UbicacionVmFalsa(espera = espera)) { vm, _ ->
            vm.abrirFormulario()
            vm.capturarUbicacion()
            advanceUntilIdle()
            vm.cerrarFormulario()
            vm.abrirFormulario()
            vm.notaCambiada("Otro")
            val nuevo = vm.state.value
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(nuevo, vm.state.value)
            assertNull(vm.state.value.formulario.ubicacion)
        }
    }

    @Test
    fun laRespuestaDeOtraObraOSesionSeDescarta() {
        val espera = CompletableDeferred<Unit>()
        conVm(ubicaciones = UbicacionVmFalsa(espera = espera)) { vm, _ ->
            vm.abrirFormulario()
            vm.capturarUbicacion()
            advanceUntilIdle()
            vm.cerrarObra()
            vm.abrirObra("otra")
            advanceUntilIdle()
            vm.abrirFormulario()
            vm.notaCambiada("Otra obra")
            val nuevo = vm.state.value
            espera.complete(Unit)
            advanceUntilIdle()
            assertEquals(nuevo, vm.state.value)
        }
    }

    @Test
    fun crearEnviaLaUbicacionCapturadaYSinCapturarNoLaPide() = conVm { vm, repo ->
        vm.abrirFormulario()
        vm.notaCambiada("Sin ubicación")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.SinCambio, repo.draft?.cambioUbicacion)
        vm.abrirFormulario()
        vm.notaCambiada("Con ubicación")
        vm.capturarUbicacion()
        advanceUntilIdle()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.Nueva(casa), repo.draft?.cambioUbicacion)
    }

    @Test
    fun editarConservaQuitaOReemplazaLaUbicacionExistente() = conVm { vm, repo ->
        val con = relevamientoCon(obra)
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(con))
        advanceUntilIdle()
        fun editarYGuardar(item: Relevamiento, cambio: RelevamientosViewModel.() -> Unit) {
            vm.abrirDetalle(item)
            vm.editarSeleccionado()
            vm.cambio()
            vm.guardar()
        }
        vm.abrirDetalle(con)
        vm.editarSeleccionado()
        assertEquals(obra, vm.state.value.formulario.ubicacion)
        vm.cerrarFormulario()
        // Sin tocarla, el campo no se escribe: la ubicación de la app anterior se conserva.
        editarYGuardar(con) { }
        advanceUntilIdle()
        assertEquals(CambioUbicacion.SinCambio, repo.draftEditado?.cambioUbicacion)
        editarYGuardar(con) { quitarUbicacion() }
        advanceUntilIdle()
        assertEquals(CambioUbicacion.Quitar, repo.draftEditado?.cambioUbicacion)
        // Capturar es asíncrono: se espera la respuesta antes de guardar.
        vm.abrirDetalle(con)
        vm.editarSeleccionado()
        vm.capturarUbicacion()
        advanceUntilIdle()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.Nueva(casa), repo.draftEditado?.cambioUbicacion)
    }

    @Test
    fun unRelevamientoSinUbicacionNoPideNadaYQuitarSinNadaQueQuitarTampoco() = conVm { vm, repo ->
        val sin = relevamientoCon(null)
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(sin))
        advanceUntilIdle()
        vm.abrirDetalle(sin)
        vm.editarSeleccionado()
        vm.quitarUbicacion()
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.SinCambio, repo.draftEditado?.cambioUbicacion)
    }

    @Test
    fun noSePuedeQuitarLaUbicacionMientrasSeBusca() {
        val espera = CompletableDeferred<Unit>()
        conVm(ubicaciones = UbicacionVmFalsa(espera = espera)) { vm, repo ->
            val item = relevamientoCon(obra)
            repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
            advanceUntilIdle()
            vm.abrirDetalle(item)
            vm.editarSeleccionado()
            vm.capturarUbicacion()
            advanceUntilIdle()
            vm.quitarUbicacion()
            assertEquals(obra, vm.state.value.formulario.ubicacion)
            espera.complete(Unit)
            advanceUntilIdle()
        }
    }

    // --- Mapa para elegir el lugar ---

    @Test
    fun elMapaSeAbreYSeCierraSinTocarLaUbicacion() = conVm { vm, _ ->
        vm.abrirFormulario()
        assertFalse(vm.state.value.formulario.mapaAbierto)
        vm.abrirMapa()
        assertTrue(vm.state.value.formulario.mapaAbierto)
        vm.cerrarMapa()
        assertFalse(vm.state.value.formulario.mapaAbierto)
        assertNull(vm.state.value.formulario.ubicacion)
    }

    @Test
    fun elegirEnElMapaCargaLaUbicacionComoNuevaCierraElMapaYElBuscador() = conVm { vm, repo ->
        vm.abrirFormulario()
        vm.notaCambiada("Con mapa")
        vm.abrirBusquedaDireccion()
        vm.textoDireccionCambiado("Calchaquí")
        vm.abrirMapa()
        vm.elegirEnMapa(casa)
        val form = vm.state.value.formulario
        assertEquals(casa, form.ubicacion)
        assertFalse(form.mapaAbierto)
        assertEquals(BusquedaDireccionUiState(disponible = true), form.busqueda)
        assertNull(form.error)
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.Nueva(casa), repo.draft?.cambioUbicacion)
    }

    @Test
    fun elegirEnElMapaReemplazaLaUbicacionDeUnRelevamientoExistente() = conVm { vm, repo ->
        val item = relevamientoCon(obra)
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(listOf(item))
        advanceUntilIdle()
        vm.abrirDetalle(item)
        vm.editarSeleccionado()
        vm.abrirMapa()
        vm.elegirEnMapa(casa)
        vm.guardar()
        advanceUntilIdle()
        assertEquals(CambioUbicacion.Nueva(casa), repo.draftEditado?.cambioUbicacion)
    }

    @Test
    fun unaEleccionSinElMapaAbiertoSeIgnora() = conVm { vm, _ ->
        vm.abrirFormulario()
        vm.elegirEnMapa(casa)
        assertNull(vm.state.value.formulario.ubicacion)
        // Y tampoco con el formulario ya cerrado.
        vm.abrirMapa()
        vm.cerrarFormulario()
        val cerrado = vm.state.value
        vm.elegirEnMapa(casa)
        assertEquals(cerrado, vm.state.value)
    }

    @Test
    fun cerrarElFormularioCierraElMapaYNoSeAbreMientrasElGpsBusca() {
        val espera = CompletableDeferred<Unit>()
        conVm(ubicaciones = UbicacionVmFalsa(espera = espera)) { vm, _ ->
            vm.abrirFormulario()
            vm.capturarUbicacion()
            runCurrent()
            vm.abrirMapa()
            assertFalse(vm.state.value.formulario.mapaAbierto)
            vm.cancelarCaptura()
            vm.abrirMapa()
            assertTrue(vm.state.value.formulario.mapaAbierto)
            vm.cerrarFormulario()
            assertFalse(vm.state.value.formulario.mapaAbierto)
            espera.complete(Unit)
            runCurrent()
        }
    }

    // --- Dirección escrita: en el detalle y buscando para elegir la ubicación ---

    private fun relevamientoConId(id: String, ubicacion: Ubicacion?) =
        Relevamiento(id, "obra", "uid", Instant.parse("2026-11-05T00:00:00Z"), "Nota $id", ubicacion = ubicacion)

    private suspend fun TestScope.conLista(repo: RelevamientosVmFalso, vararg items: Relevamiento) {
        repo.stream("uid", "obra").value = ResultadoRelevamiento.Exito(items.toList())
        advanceUntilIdle()
    }

    @Test
    fun elDetalleCargaLaDireccionAparteYConservaLasCoordenadas() {
        val direcciones = DireccionesVmFalsa()
        conVm(direcciones = direcciones) { vm, repo ->
            val item = relevamientoCon(obra)
            conLista(repo, item)
            vm.abrirDetalle(item)
            assertEquals(DireccionUiState.Cargando, vm.state.value.direccion)
            advanceUntilIdle()
            assertEquals(DireccionUiState.Disponible("Av. Calchaquí 6200"), vm.state.value.direccion)
            assertEquals(obra, vm.state.value.seleccionado?.ubicacion)
            assertEquals(listOf(obra), direcciones.direccionesPedidas)
        }
    }

    @Test
    fun sinUbicacionNoSeConsultaLaDireccion() {
        val direcciones = DireccionesVmFalsa()
        conVm(direcciones = direcciones) { vm, repo ->
            val item = relevamientoCon(null)
            conLista(repo, item)
            vm.abrirDetalle(item)
            advanceUntilIdle()
            assertEquals(DireccionUiState.Ninguna, vm.state.value.direccion)
            assertTrue(direcciones.direccionesPedidas.isEmpty())
        }
    }

    @Test
    fun siLaDireccionFallaOElDispositivoNoGeocodificaElDetalleSigueConCoordenadasSinError() {
        val casos = listOf<() -> ResultadoDireccion<String>>(
            { ResultadoDireccion.NoDisponible }, { ResultadoDireccion.NoSoportado }, { ResultadoDireccion.SinResultados },
            { throw IllegalStateException("el sistema falló") },
        )
        for (caso in casos) {
            conVm(direcciones = DireccionesVmFalsa(direccion = { caso() })) { vm, repo ->
                val item = relevamientoCon(obra)
                conLista(repo, item)
                vm.abrirDetalle(item)
                advanceUntilIdle()
                assertEquals(DireccionUiState.NoDisponible, vm.state.value.direccion)
                assertEquals(obra, vm.state.value.seleccionado?.ubicacion)
                assertNull(vm.state.value.errorOperacion)
            }
        }
    }

    @Test
    fun unaDireccionTardiaDeOtroRelevamientoOCerradoElDetalleSeDescarta() {
        val direcciones = DireccionesVmFalsa(direccion = { if (it == obra) ResultadoDireccion.Exito("Calle de la obra") else ResultadoDireccion.Exito("Calle de la casa") })
        conVm(direcciones = direcciones) { vm, repo ->
            val unoEnObra = relevamientoConId("a", obra)
            val otroEnCasa = relevamientoConId("b", casa)
            conLista(repo, unoEnObra, otroEnCasa)
            direcciones.esperaProxima = CompletableDeferred()
            val tardia = direcciones.esperaProxima!!
            vm.abrirDetalle(unoEnObra)
            runCurrent()
            vm.abrirDetalle(otroEnCasa)
            runCurrent()
            assertEquals(DireccionUiState.Disponible("Calle de la casa"), vm.state.value.direccion)
            tardia.complete(Unit)
            runCurrent()
            assertEquals(DireccionUiState.Disponible("Calle de la casa"), vm.state.value.direccion)
            vm.cerrarDetalle()
            assertEquals(DireccionUiState.Ninguna, vm.state.value.direccion)
            advanceUntilIdle()
            assertEquals(DireccionUiState.Ninguna, vm.state.value.direccion)
        }
    }

    @Test
    fun laDireccionSeRecargaSoloSiCambiaLaUbicacionDelRelevamientoAbierto() {
        val direcciones = DireccionesVmFalsa(direccion = { ResultadoDireccion.Exito(if (it == obra) "Calle de la obra" else "Calle de la casa") })
        conVm(direcciones = direcciones) { vm, repo ->
            val item = relevamientoCon(obra)
            conLista(repo, item)
            vm.abrirDetalle(item)
            advanceUntilIdle()
            // Otra emisión del listado con la misma ubicación no vuelve a consultar.
            conLista(repo, item.copy(nota = "Otra nota"))
            assertEquals(1, direcciones.direccionesPedidas.size)
            conLista(repo, item.copy(ubicacion = casa))
            assertEquals(listOf(obra, casa), direcciones.direccionesPedidas)
            assertEquals(DireccionUiState.Disponible("Calle de la casa"), vm.state.value.direccion)
            // Si el relevamiento desaparece, la dirección también.
            conLista(repo)
            assertEquals(DireccionUiState.Ninguna, vm.state.value.direccion)
        }
    }

    @Test
    fun elBuscadorSoloSeOfreceSiElDispositivoGeocodifica() {
        conVm(direcciones = DireccionesVmFalsa(soportado = false)) { vm, _ ->
            vm.abrirFormulario()
            assertFalse(vm.state.value.formulario.busqueda.disponible)
            vm.abrirBusquedaDireccion()
            assertFalse(vm.state.value.formulario.busqueda.abierta)
        }
        conVm { vm, _ ->
            vm.abrirFormulario()
            assertTrue(vm.state.value.formulario.busqueda.disponible)
            vm.abrirBusquedaDireccion()
            assertTrue(vm.state.value.formulario.busqueda.abierta)
        }
    }

    @Test
    fun conMenosDeTresCaracteresNoSePuedeBuscar() {
        val direcciones = DireccionesVmFalsa()
        conVm(direcciones = direcciones) { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("a b")
            assertFalse(vm.state.value.formulario.busqueda.puedeBuscar)
            vm.buscarDireccion()
            advanceUntilIdle()
            assertTrue(direcciones.busquedasPedidas.isEmpty())
            assertEquals(ResultadosBusquedaUi.SinBuscar, vm.state.value.formulario.busqueda.resultados)
            vm.textoDireccionCambiado("abc")
            assertTrue(vm.state.value.formulario.busqueda.puedeBuscar)
        }
    }

    @Test
    fun buscaConElTextoNormalizadoYMuestraLosCandidatos() {
        val direcciones = DireccionesVmFalsa()
        conVm(direcciones = direcciones) { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("  Av.   Calchaquí  6200 ")
            vm.buscarDireccion()
            assertEquals(ResultadosBusquedaUi.Buscando, vm.state.value.formulario.busqueda.resultados)
            advanceUntilIdle()
            assertEquals(listOf("Av. Calchaquí 6200"), direcciones.busquedasPedidas)
            assertEquals(ResultadosBusquedaUi.Lista(CANDIDATOS_VM), vm.state.value.formulario.busqueda.resultados)
        }
    }

    @Test
    fun elegirUnCandidatoCargaLaUbicacionComoNuevaYSeGuarda() {
        conVm { vm, repo ->
            vm.abrirFormulario()
            vm.notaCambiada("Con dirección")
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            vm.buscarDireccion()
            advanceUntilIdle()
            val elegido = CANDIDATOS_VM[2]
            vm.elegirDireccion(elegido)
            val form = vm.state.value.formulario
            assertEquals(elegido.ubicacion, form.ubicacion)
            assertEquals(BusquedaDireccionUiState(disponible = true), form.busqueda)
            vm.guardar()
            advanceUntilIdle()
            assertEquals(CambioUbicacion.Nueva(elegido.ubicacion), repo.draft?.cambioUbicacion)
        }
    }

    @Test
    fun soloSePuedeElegirUnCandidatoQueSeEsteMostrando() {
        conVm { vm, _ ->
            vm.abrirFormulario()
            vm.elegirDireccion(CANDIDATOS_VM[0])
            assertNull(vm.state.value.formulario.ubicacion)
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            vm.buscarDireccion()
            advanceUntilIdle()
            vm.elegirDireccion(CandidatoDireccion("Otra calle", Ubicacion(1.0, 1.0)))
            assertNull(vm.state.value.formulario.ubicacion)
        }
    }

    @Test
    fun unaBusquedaNuevaReemplazaALaAnteriorYLaTardiaSeDescarta() {
        val direcciones = DireccionesVmFalsa(busqueda = { ResultadoDireccion.Exito(listOf(CandidatoDireccion("Resultado de $it", casa))) })
        conVm(direcciones = direcciones) { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("primera")
            direcciones.esperaProxima = CompletableDeferred()
            val tardia = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            vm.textoDireccionCambiado("segunda")
            vm.buscarDireccion()
            runCurrent()
            val esperado = ResultadosBusquedaUi.Lista(listOf(CandidatoDireccion("Resultado de segunda", casa)))
            assertEquals(esperado, vm.state.value.formulario.busqueda.resultados)
            tardia.complete(Unit)
            runCurrent()
            assertEquals(esperado, vm.state.value.formulario.busqueda.resultados)
            assertEquals(listOf("primera", "segunda"), direcciones.busquedasPedidas)
        }
    }

    @Test
    fun laRespuestaTardiaDeLaPrimeraBusquedaNoSeMuestraComoSiFueraDeLaSegunda() {
        val direcciones = DireccionesVmFalsa(busqueda = { ResultadoDireccion.Exito(listOf(CandidatoDireccion("Resultado de $it", casa))) })
        conVm(direcciones = direcciones) { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("primera")
            direcciones.esperaProxima = CompletableDeferred()
            val primera = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            vm.textoDireccionCambiado("segunda")
            direcciones.esperaProxima = CompletableDeferred()
            val segunda = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            // La segunda sigue buscando: lo que llegue de la primera no es su resultado.
            primera.complete(Unit)
            runCurrent()
            assertEquals(ResultadosBusquedaUi.Buscando, vm.state.value.formulario.busqueda.resultados)
            segunda.complete(Unit)
            runCurrent()
            assertEquals(
                ResultadosBusquedaUi.Lista(listOf(CandidatoDireccion("Resultado de segunda", casa))),
                vm.state.value.formulario.busqueda.resultados,
            )
        }
    }

    @Test
    fun cerrarElBuscadorElFormularioOCambiarDeObraDescartaLaRespuestaTardia() {
        val direcciones = DireccionesVmFalsa()
        conVm(direcciones = direcciones) { vm, _ ->
            // Cerrar el buscador.
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            direcciones.esperaProxima = CompletableDeferred()
            var tardia = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            vm.cerrarBusquedaDireccion()
            val cerrado = vm.state.value
            tardia.complete(Unit)
            runCurrent()
            assertEquals(cerrado, vm.state.value)
            // Cerrar y reabrir el formulario.
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            direcciones.esperaProxima = CompletableDeferred()
            tardia = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            vm.cerrarFormulario()
            vm.abrirFormulario()
            vm.notaCambiada("Nuevo")
            val reabierto = vm.state.value
            tardia.complete(Unit)
            runCurrent()
            assertEquals(reabierto, vm.state.value)
            // Cambiar de obra.
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            direcciones.esperaProxima = CompletableDeferred()
            tardia = direcciones.esperaProxima!!
            vm.buscarDireccion()
            runCurrent()
            vm.cerrarObra()
            vm.abrirObra("otra")
            runCurrent()
            vm.abrirFormulario()
            vm.notaCambiada("Otra obra")
            val otraObra = vm.state.value
            tardia.complete(Unit)
            runCurrent()
            assertEquals(otraObra, vm.state.value)
        }
    }

    @Test
    fun cambiarElTextoDescartaLosResultadosDelTextoAnterior() {
        conVm { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            vm.buscarDireccion()
            advanceUntilIdle()
            assertTrue(vm.state.value.formulario.busqueda.resultados is ResultadosBusquedaUi.Lista)
            vm.textoDireccionCambiado("Calchaquí 6200")
            assertEquals(ResultadosBusquedaUi.SinBuscar, vm.state.value.formulario.busqueda.resultados)
        }
    }

    @Test
    fun cadaResultadoDeLaBusquedaSeMuestraSinBloquearElGuardado() {
        val casos = listOf<Pair<() -> ResultadoDireccion<List<CandidatoDireccion>>, ResultadosBusquedaUi>>(
            { ResultadoDireccion.SinResultados } to ResultadosBusquedaUi.SinResultados,
            { ResultadoDireccion.NoDisponible } to ResultadosBusquedaUi.NoDisponible,
            { throw IllegalStateException("el sistema falló") } to ResultadosBusquedaUi.NoDisponible,
        )
        for ((respuesta, esperado) in casos) {
            conVm(direcciones = DireccionesVmFalsa(busqueda = { respuesta() })) { vm, repo ->
                vm.abrirFormulario()
                vm.notaCambiada("Se guarda igual")
                vm.abrirBusquedaDireccion()
                vm.textoDireccionCambiado("Calchaquí")
                vm.buscarDireccion()
                advanceUntilIdle()
                assertEquals(esperado, vm.state.value.formulario.busqueda.resultados)
                assertNull(vm.state.value.formulario.error)
                vm.guardar()
                advanceUntilIdle()
                assertEquals(1, repo.altas)
                assertEquals(CambioUbicacion.SinCambio, repo.draft?.cambioUbicacion)
            }
        }
    }

    @Test
    fun siElDispositivoNoGeocodificaElBuscadorSeOcultaSinError() {
        conVm(direcciones = DireccionesVmFalsa(busqueda = { ResultadoDireccion.NoSoportado })) { vm, _ ->
            vm.abrirFormulario()
            vm.abrirBusquedaDireccion()
            vm.textoDireccionCambiado("Calchaquí")
            vm.buscarDireccion()
            advanceUntilIdle()
            val form = vm.state.value.formulario
            assertEquals(BusquedaDireccionUiState(), form.busqueda)
            assertFalse(form.busqueda.disponible)
            assertNull(form.error)
        }
    }
}

private val CANDIDATOS_VM = listOf(
    CandidatoDireccion("Av. Calchaquí 6200, Florencio Varela", Ubicacion(-34.7746, -58.2672)),
    CandidatoDireccion("Calchaquí 100, Quilmes", Ubicacion(-34.72, -58.25)),
    CandidatoDireccion("Calchaquí 2500, Quilmes", Ubicacion(-34.73, -58.26)),
    CandidatoDireccion("Av. Calchaquí 1, Berazategui", Ubicacion(-34.76, -58.21)),
    CandidatoDireccion("Calchaquí s/n, Berazategui", Ubicacion(-34.77, -58.22)),
)

private class DireccionesVmFalsa(
    private val soportado: Boolean = true,
    private val direccion: (Ubicacion) -> ResultadoDireccion<String> = { ResultadoDireccion.Exito("Av. Calchaquí 6200") },
    private val busqueda: (String) -> ResultadoDireccion<List<CandidatoDireccion>> = { ResultadoDireccion.Exito(CANDIDATOS_VM) },
) : ProveedorDirecciones {
    val direccionesPedidas = mutableListOf<Ubicacion>()
    val busquedasPedidas = mutableListOf<String>()

    /** La próxima consulta responde recién cuando se completa, y aunque la hayan cancelado: el sistema no la corta. */
    var esperaProxima: CompletableDeferred<Unit>? = null

    private suspend fun esperar() {
        val espera = esperaProxima ?: return
        esperaProxima = null
        try { withContext(NonCancellable) { espera.await() } } catch (_: CancellationException) { }
    }

    override fun estaSoportado() = soportado

    override suspend fun direccionDe(ubicacion: Ubicacion): ResultadoDireccion<String> {
        direccionesPedidas += ubicacion
        esperar()
        return direccion(ubicacion)
    }

    override suspend fun buscar(texto: String): ResultadoDireccion<List<CandidatoDireccion>> {
        busquedasPedidas += texto
        esperar()
        return busqueda(texto)
    }
}

private class UbicacionVmFalsa(
    private val espera: CompletableDeferred<Unit>? = null,
    private val posicion: () -> ResultadoUbicacion = { ResultadoUbicacion.Exito(Ubicacion(-34.7746, -58.2672)) },
) : ProveedorUbicacion {
    var busquedas = 0
    override suspend fun solicitarAcceso(): ResultadoUbicacion.Fallo? = null
    override suspend fun posicionActual(): ResultadoUbicacion {
        busquedas++
        // La respuesta llega aunque la búsqueda ya se haya cancelado, como el diálogo de permiso de Android.
        espera?.let { try { withContext(NonCancellable) { it.await() } } catch (_: CancellationException) { } }
        return posicion()
    }
}

private class FotosVmFalso(
    private val resultado: ResultadoSubidaFoto = ResultadoSubidaFoto.Exito("https://foto"),
    private val espera: CompletableDeferred<Unit>? = null,
) : AlmacenFotos {
    var subidas = 0
    override suspend fun subir(bytes: ByteArray, carpeta: String, nombre: String): ResultadoSubidaFoto {
        subidas++
        espera?.let { try { withContext(NonCancellable) { it.await() } } catch (_: CancellationException) { } }
        return resultado
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
    var falloCambio: ErrorRelevamiento? = null
    var esperaCambio: CompletableDeferred<Unit>? = null
    var eliminado: String? = null
    var espera: CompletableDeferred<Unit>? = null
    var altas = 0
    var draft: RelevamientoDraft? = null
    var fotoUrl: String? = null
    var cambioFoto: CambioFotoUrl? = null
    var draftEditado: RelevamientoDraft? = null
    fun stream(uid: String, obra: String) = streams.getOrPut(uid to obra) { MutableStateFlow(ResultadoRelevamiento.Exito(emptyList())) }
    override fun observarDeObra(userId: String, obraId: String) = stream(userId, obraId).also { consultas += userId to obraId }
    override suspend fun crear(userId: String, obraId: String, draft: RelevamientoDraft, fotoUrl: String?): ResultadoRelevamiento<Relevamiento> {
        altas++
        this.draft = draft
        this.fotoUrl = fotoUrl
        espera?.let {
            try { withContext(NonCancellable) { it.await() } } catch (_: CancellationException) { }
        }
        falloAlta?.let { return ResultadoRelevamiento.Fallo(it) }
        val item = Relevamiento("r-$altas", obraId, userId, draft.fecha, draft.nota, draft.tareaId)
        stream(userId, obraId).value = ResultadoRelevamiento.Exito(listOf(item))
        return ResultadoRelevamiento.Exito(item)
    }
    private suspend fun esperarCambio() {
        esperaCambio?.let { try { withContext(NonCancellable) { it.await() } } catch (_: CancellationException) { } }
    }
    override suspend fun actualizar(
        userId: String, obraId: String, id: String, draft: RelevamientoDraft, foto: CambioFotoUrl,
    ): ResultadoRelevamiento<Unit> {
        cambioFoto = foto
        draftEditado = draft
        esperarCambio()
        falloCambio?.let { return ResultadoRelevamiento.Fallo(it) }
        val previo = (stream(userId, obraId).value as ResultadoRelevamiento.Exito).dato
        stream(userId, obraId).value = ResultadoRelevamiento.Exito(previo.map {
            if (it.id == id) it.copy(fecha = draft.fecha, nota = draft.nota, tareaId = draft.tareaId) else it
        })
        return ResultadoRelevamiento.Exito(Unit)
    }
    override suspend fun eliminar(userId: String, obraId: String, id: String): ResultadoRelevamiento<Unit> {
        eliminado = id
        if (falloCambio == null) {
            val previo = (stream(userId, obraId).value as ResultadoRelevamiento.Exito).dato
            stream(userId, obraId).value = ResultadoRelevamiento.Exito(previo.filterNot { it.id == id })
        }
        esperarCambio()
        return falloCambio?.let { ResultadoRelevamiento.Fallo(it) } ?: ResultadoRelevamiento.Exito(Unit)
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
