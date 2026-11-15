package com.unaj.obratrack.features.obras

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.PlantillasDeTareas
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.model.TareaPlantilla
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.repository.SugeridorDeTareas
import com.unaj.obratrack.domain.usecase.CrearTarea
import com.unaj.obratrack.domain.usecase.CrearTareasSugeridas
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.SugerirTareas
import com.unaj.obratrack.domain.usecase.SugerirTareasConIa
import com.unaj.obratrack.domain.repository.SugeridorConIA
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SugerenciasViewModelTest {
    private fun conViewModel(
        repo: TareasQueGuardan = TareasQueGuardan(),
        sugeridor: SugeridorFalso = SugeridorFalso(),
        usuario: Usuario? = Usuario("uid-1", "a@b.com", null),
        ia: IaFalsa? = null,
        bloque: suspend TestScope.(SugerenciasViewModel, TareasQueGuardan) -> Unit,
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = SugerenciasViewModel(
            ObservarSesion(SesionDeSugerencias(usuario)),
            SugerirTareas(sugeridor),
            CrearTareasSugeridas(CrearTarea(repo)),
            ia?.let { SugerirTareasConIa(it) },
        )
        try {
            bloque(viewModel, repo)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private val casa = PlantillasDeTareas.para(TipoObra.PINTURA).map { SugerenciaTarea(TituloSugerido.Plantilla(it), it.categoria) }

    private fun titulosDe(sugerencias: List<SugerenciaTarea>) = sugerencias.associateWith { s ->
        (s.titulo as TituloSugerido.Plantilla).tarea.name.lowercase()
    }

    @Test
    fun abrirMuestraLaPantallaSinTipoElegido() = conViewModel { vm, _ ->
        vm.abrir("obra-1")

        assertTrue(vm.state.value.visible)
        assertNull(vm.state.value.tipo)
        assertTrue(vm.state.value.sugerencias.isEmpty())
    }

    @Test
    fun abrirDesdeLaAltaDeObraSeMarcaYAlReabrirDesdeLaObraNo() = conViewModel { vm, _ ->
        vm.abrir("obra-1", altaDeObra = true)
        assertTrue(vm.state.value.altaDeObra)

        vm.cerrar()
        assertFalse(vm.state.value.altaDeObra)

        vm.abrir("obra-1")
        assertFalse(vm.state.value.altaDeObra)
    }

    @Test
    fun alElegirUnTipoSeListanTodasLasSugerenciasYQuedanElegidas() = conViewModel { vm, _ ->
        vm.abrir("obra-1")

        vm.elegirTipo(TipoObra.PINTURA)

        val estado = vm.state.value
        assertEquals(TipoObra.PINTURA, estado.tipo)
        assertEquals(casa, estado.sugerencias)
        assertEquals(casa.toSet(), estado.seleccionadas)
        assertFalse(estado.cargando)
    }

    @Test
    fun elegirOtroTipoReemplazaLaListaYLaSeleccion() = conViewModel { vm, _ ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.CASA_NUEVA)
        vm.alternar(vm.state.value.sugerencias.first())

        vm.elegirTipo(TipoObra.PINTURA)

        assertEquals(casa, vm.state.value.sugerencias)
        assertEquals(casa.toSet(), vm.state.value.seleccionadas)
    }

    @Test
    fun alternarYSeleccionarTodasCambianLaSeleccion() = conViewModel { vm, _ ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)

        vm.alternar(casa.first())
        assertFalse(casa.first() in vm.state.value.seleccionadas)
        vm.alternar(casa.first())
        assertTrue(casa.first() in vm.state.value.seleccionadas)
        vm.seleccionarTodas(false)
        assertTrue(vm.state.value.seleccionadas.isEmpty())
        assertFalse(vm.state.value.puedeCrear)
        vm.seleccionarTodas(true)
        assertEquals(casa.toSet(), vm.state.value.seleccionadas)
    }

    @Test
    fun crearGuardaSoloLasElegidasEnOrdenConElTituloTraducidoYCierra() = conViewModel { vm, repo ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)
        vm.alternar(casa[1])

        vm.crear(titulosDe(vm.state.value.sugerencias))

        assertEquals(listOf("revoques", "pintura_exterior", "limpieza_final"), repo.titulos)
        assertEquals(listOf(CategoriaTarea.ALBANILERIA, CategoriaTarea.PINTURA, CategoriaTarea.LIMPIEZA), repo.categorias)
        assertEquals("obra-1", repo.obraId)
        assertEquals("uid-1", repo.userId)
        assertFalse(vm.state.value.visible)
    }

    @Test
    fun siFallaAMitadQuedanFueraLasGuardadasYSePuedeReintentarSinDuplicar() = conViewModel(
        repo = TareasQueGuardan(fallarEnLaLlamada = 2),
    ) { vm, repo ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)

        vm.crear(titulosDe(vm.state.value.sugerencias))

        val estado = vm.state.value
        assertTrue(estado.visible)
        assertEquals(ErrorSugerenciasUi.RED, estado.error)
        assertEquals(1, estado.creadasAntesDelError)
        assertEquals(casa.drop(1), estado.sugerencias)
        assertEquals(casa.drop(1).toSet(), estado.seleccionadas)
        assertFalse(estado.creando)

        repo.fallarEnLaLlamada = null
        vm.crear(titulosDe(vm.state.value.sugerencias))

        assertEquals(listOf("revoques", "pintura_interior", "pintura_exterior", "limpieza_final"), repo.titulos)
        assertFalse(vm.state.value.visible)
    }

    @Test
    fun noSeCreaDosVecesMientrasSeGuarda() {
        val compuerta = CompletableDeferred<Unit>()
        conViewModel(repo = TareasQueGuardan(espera = compuerta)) { vm, repo ->
            vm.abrir("obra-1")
            vm.elegirTipo(TipoObra.PINTURA)
            val titulos = titulosDe(vm.state.value.sugerencias)

            vm.crear(titulos)
            vm.crear(titulos)
            assertTrue(vm.state.value.creando)
            compuerta.complete(Unit)

            assertEquals(4, repo.titulos.size)
        }
    }

    @Test
    fun mientrasSeGuardaNoSePuedeCambiarLaSeleccionNiElTipo() {
        val compuerta = CompletableDeferred<Unit>()
        conViewModel(repo = TareasQueGuardan(espera = compuerta)) { vm, _ ->
            vm.abrir("obra-1")
            vm.elegirTipo(TipoObra.PINTURA)
            vm.crear(titulosDe(vm.state.value.sugerencias))

            vm.alternar(casa.first())
            vm.seleccionarTodas(false)
            vm.elegirTipo(TipoObra.BANO)

            assertEquals(TipoObra.PINTURA, vm.state.value.tipo)
            assertEquals(casa.toSet(), vm.state.value.seleccionadas)
            compuerta.complete(Unit)
        }
    }

    @Test
    fun siFallaElSugeridorSeInformaYNoHayLista() = conViewModel(
        sugeridor = SugeridorFalso(ResultadoSugerir.Fallo(ErrorSugerir.LIMITE_ALCANZADO)),
    ) { vm, _ ->
        vm.abrir("obra-1")

        vm.elegirTipo(TipoObra.CASA_NUEVA)

        assertEquals(ErrorSugerenciasUi.LIMITE, vm.state.value.error)
        assertTrue(vm.state.value.sugerencias.isEmpty())
        assertFalse(vm.state.value.cargando)
    }

    @Test
    fun sinSesionNoCreaNadaEInforma() = conViewModel(usuario = null) { vm, repo ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)

        vm.crear(titulosDe(vm.state.value.sugerencias))

        assertEquals(ErrorSugerenciasUi.SESION, vm.state.value.error)
        assertTrue(repo.titulos.isEmpty())
    }

    private val deLaIa = listOf(
        SugerenciaTarea(TituloSugerido.Libre("Tendido de cañerías"), CategoriaTarea.PLOMERIA),
        SugerenciaTarea(TituloSugerido.Libre("Pintura de muros"), CategoriaTarea.PINTURA),
    )

    @Test
    fun sinServicioDeIaNoSeOfreceNiHaceNada() = conViewModel { vm, _ ->
        vm.abrir("obra-1")
        vm.descripcionCambiada("Casa de dos plantas")

        vm.sugerirConIa()

        assertFalse(vm.state.value.iaDisponible)
        assertFalse(vm.state.value.puedeSugerirConIa)
        assertTrue(vm.state.value.sugerencias.isEmpty())
    }

    @Test
    fun conServicioDeIaSeOfreceDesdeElPrincipio() = conViewModel(ia = IaFalsa()) { vm, _ ->
        assertTrue(vm.state.value.iaDisponible)
        vm.abrir("obra-1")
        assertTrue(vm.state.value.iaDisponible)
        vm.cerrar()
        assertTrue(vm.state.value.iaDisponible)
    }

    @Test
    fun laDescripcionSeLimitaAlLargoMaximo() = conViewModel(ia = IaFalsa()) { vm, _ ->
        vm.abrir("obra-1")

        vm.descripcionCambiada("a".repeat(600))

        assertEquals(500, vm.state.value.descripcion.length)
    }

    @Test
    fun sinDescripcionNoSePuedeSugerirConIa() = conViewModel(ia = IaFalsa()) { vm, _ ->
        vm.abrir("obra-1")
        vm.descripcionCambiada("   ")

        vm.sugerirConIa()

        assertFalse(vm.state.value.puedeSugerirConIa)
        assertTrue(vm.state.value.sugerencias.isEmpty())
    }

    @Test
    fun laIaReemplazaLaListaYQuedanTodasElegidas() {
        val ia = IaFalsa(ResultadoSugerir.Exito(deLaIa))
        conViewModel(ia = ia) { vm, _ ->
            vm.abrir("obra-1")
            vm.elegirTipo(TipoObra.PINTURA)
            vm.descripcionCambiada("  Baño con pileta  ")

            vm.sugerirConIa()

            val estado = vm.state.value
            assertEquals(deLaIa, estado.sugerencias)
            assertEquals(deLaIa.toSet(), estado.seleccionadas)
            assertNull(estado.tipo)
            assertFalse(estado.cargando)
            assertEquals("Baño con pileta", ia.ultimaDescripcion)
        }
    }

    @Test
    fun siFallaLaIaSeConservaLaListaDeLaPlantilla() = conViewModel(
        ia = IaFalsa(ResultadoSugerir.Fallo(ErrorSugerir.SIN_CONEXION)),
    ) { vm, _ ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)
        vm.descripcionCambiada("Baño con pileta")

        vm.sugerirConIa()

        val estado = vm.state.value
        assertEquals(ErrorSugerenciasUi.RED, estado.error)
        assertEquals(casa, estado.sugerencias)
        assertEquals(TipoObra.PINTURA, estado.tipo)
        assertFalse(estado.cargando)
    }

    @Test
    fun elLimiteDeUsoSeInforma() = conViewModel(
        ia = IaFalsa(ResultadoSugerir.Fallo(ErrorSugerir.LIMITE_ALCANZADO)),
    ) { vm, _ ->
        vm.abrir("obra-1")
        vm.descripcionCambiada("Baño")

        vm.sugerirConIa()

        assertEquals(ErrorSugerenciasUi.LIMITE, vm.state.value.error)
    }

    @Test
    fun lasTareasDeLaIaSeCreanConSuTituloYCategoria() {
        val ia = IaFalsa(ResultadoSugerir.Exito(deLaIa))
        conViewModel(ia = ia) { vm, repo ->
            vm.abrir("obra-1")
            vm.descripcionCambiada("Baño")
            vm.sugerirConIa()

            vm.crear(vm.state.value.sugerencias.associateWith { (it.titulo as TituloSugerido.Libre).texto })

            assertEquals(listOf("Tendido de cañerías", "Pintura de muros"), repo.titulos)
            assertEquals(listOf(CategoriaTarea.PLOMERIA, CategoriaTarea.PINTURA), repo.categorias)
            assertFalse(vm.state.value.visible)
        }
    }

    @Test
    fun noSeSugiereConIaMientrasSeGuarda() {
        val compuerta = CompletableDeferred<Unit>()
        val ia = IaFalsa()
        conViewModel(repo = TareasQueGuardan(espera = compuerta), ia = ia) { vm, _ ->
            vm.abrir("obra-1")
            vm.elegirTipo(TipoObra.PINTURA)
            vm.crear(titulosDe(vm.state.value.sugerencias))
            vm.descripcionCambiada("Baño")

            vm.sugerirConIa()

            assertEquals(0, ia.llamadas)
            compuerta.complete(Unit)
        }
    }

    @Test
    fun cerrarDescartaTodo() = conViewModel { vm, _ ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)

        vm.cerrar()

        assertEquals(SugerenciasUiState(), vm.state.value)
    }

    @Test
    fun alReabrirNoQuedaNadaDeLaVezAnterior() = conViewModel { vm, _ ->
        vm.abrir("obra-1")
        vm.elegirTipo(TipoObra.PINTURA)
        vm.cerrar()

        vm.abrir("obra-2")

        assertNull(vm.state.value.tipo)
        assertTrue(vm.state.value.sugerencias.isEmpty())
    }
}

private class SugeridorFalso(
    private val resultado: ResultadoSugerir? = null,
) : SugeridorDeTareas {
    override suspend fun sugerir(tipo: TipoObra): ResultadoSugerir = resultado
        ?: ResultadoSugerir.Exito(
            PlantillasDeTareas.para(tipo).map { SugerenciaTarea(TituloSugerido.Plantilla(it), it.categoria) },
        )
}

private class SesionDeSugerencias(usuario: Usuario?) : RepositorioAutenticacion {
    override val usuarioActual = MutableStateFlow(usuario)
    override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
}

/** Guarda lo que recibe, puede fallar desde una llamada y puede quedarse esperando a que se libere [espera]. */
private class TareasQueGuardan(
    var fallarEnLaLlamada: Int? = null,
    private val espera: CompletableDeferred<Unit>? = null,
) : RepositorioTareas {
    val titulos = mutableListOf<String>()
    val categorias = mutableListOf<CategoriaTarea>()
    var userId: String? = null
    var obraId: String? = null
    private var llamadas = 0

    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> {
        llamadas++
        espera?.await()
        if (fallarEnLaLlamada != null && llamadas >= fallarEnLaLlamada!!) return ResultadoTarea.Fallo(ErrorTarea.RED)
        this.userId = userId
        this.obraId = obraId
        titulos += draft.titulo
        categorias += draft.categoria
        return ResultadoTarea.Exito(Tarea("t$llamadas", obraId, userId, draft.titulo, categoria = draft.categoria))
    }

    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> = emptyFlow()
    override suspend fun actualizar(userId: String, obraId: String, tareaId: String, draft: TareaDraft) = unsupported()
    override suspend fun cambiarEstado(userId: String, obraId: String, tareaId: String, estado: EstadoTarea) = unsupported()
    override suspend fun eliminar(userId: String, obraId: String, tareaId: String, vinculados: RelevamientosVinculados) =
        unsupported()

    private fun unsupported(): Nothing = error("no se usa")
}

internal class IaFalsa(private val resultado: ResultadoSugerir = ResultadoSugerir.Exito(emptyList())) : SugeridorConIA {
    var llamadas = 0
    var ultimaDescripcion: String? = null

    override suspend fun sugerir(descripcion: String): ResultadoSugerir {
        llamadas++
        ultimaDescripcion = descripcion
        return resultado
    }
}
