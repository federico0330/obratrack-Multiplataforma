package com.unaj.obratrack.features.obras

import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.ObraDraft
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorObra
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoObra
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.usecase.CrearObra
import com.unaj.obratrack.domain.usecase.ActualizarObra
import com.unaj.obratrack.domain.usecase.CambiarEstadoObra
import com.unaj.obratrack.domain.usecase.EliminarObra
import com.unaj.obratrack.domain.usecase.ProgramarVisita
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant
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
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ObrasViewModelTest {
    @Test
    fun creaObraConUsuarioDeSesionYDeshabilitaFormularioAlCompletar() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val session = AuthRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(session), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
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
                ObservarSesion(session), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
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
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
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
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
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

    @Test
    fun soloEliminaAlConfirmarYUsaLaSesionActual() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
            )
            advanceUntilIdle()
            viewModel.abrirDetalle(obra("obra-1", "uid-1", "Casa"))
            viewModel.solicitarEliminacion()
            viewModel.cancelarEliminacion()
            advanceUntilIdle()
            assertNull(repo.deletedObraId)

            viewModel.solicitarEliminacion()
            viewModel.confirmarEliminacion()
            advanceUntilIdle()

            assertEquals("uid-1", repo.deletedUserId)
            assertEquals("obra-1", repo.deletedObraId)
            assertNull(viewModel.state.value.obraSeleccionada)
            assertFalse(viewModel.state.value.eliminando)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun programaReprogramaYQuitaLaVisitaConFechaUtcYHoraOpcional() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo),
                ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
            )
            advanceUntilIdle()
            val obra = obra("obra-1", "uid-1", "Casa")
            val primeraFecha = LocalDate(2026, 11, 10)
            val segundaFecha = LocalDate(2026, 11, 12)

            viewModel.editarVisita(obra)
            viewModel.guardarVisita(primeraFecha, LocalTime(0, 0))
            viewModel.guardarVisita(primeraFecha, LocalTime(12, 0))
            assertTrue(viewModel.state.value.edicionVisita?.guardando == true)
            advanceUntilIdle()
            assertNull(viewModel.state.value.edicionVisita)
            assertEquals(Instant.parse("2026-11-10T00:00:00Z") to 0, repo.visitas.single())

            viewModel.editarVisita(obra)
            viewModel.guardarVisita(segundaFecha, null)
            advanceUntilIdle()
            viewModel.editarVisita(obra)
            viewModel.guardarVisita(null, LocalTime(23, 59))
            advanceUntilIdle()
            assertEquals(
                listOf(
                    Instant.parse("2026-11-10T00:00:00Z") to 0,
                    Instant.parse("2026-11-12T00:00:00Z") to null,
                    null to null,
                ),
                repo.visitas,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mantieneLaEdicionSiFallaLaEscrituraYLimpiaAlCambiarDeSesion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            repo.resultadoVisita = ResultadoObra.Fallo(ErrorObra.RED)
            val session = AuthRepoFalso()
            val viewModel = ObrasViewModel(
                ObservarSesion(session), ObservarObras(repo), CrearObra(repo),
                ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
            )
            advanceUntilIdle()

            val obra = obra("obra-1", "uid-1", "Casa")
            viewModel.editarVisita(obra)
            viewModel.guardarVisita(LocalDate(2026, 11, 10), null)
            advanceUntilIdle()
            assertEquals(EdicionVisitaUiState(obra, error = ErrorObrasUi.RED), viewModel.state.value.edicionVisita)

            session.usuarioActual.value = Usuario("uid-2", "otra@example.com", null)
            advanceUntilIdle()
            assertNull(viewModel.state.value.edicionVisita)
            viewModel.editarVisita(obra)
            assertNull(viewModel.state.value.edicionVisita)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aceptaHorasDeVeinticuatroHorasConMinutosValidos() {
        assertEquals(LocalTime(0, 0), horaVisitaDesdeTexto("00:00"))
        assertEquals(LocalTime(23, 59), horaVisitaDesdeTexto("23:59"))
        for (hora in listOf("", "09", "9:30", "24:00", "12:60", "12:30:00")) {
            assertNull(horaVisitaDesdeTexto(hora))
        }
    }

    @Test
    fun reintentarReabreLaSuscripcionAunqueLaAnteriorSigaActiva() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoFalso()
            repo.flujoObras = MutableStateFlow(ResultadoObra.Fallo(ErrorObra.RED))
            val viewModel = ObrasViewModel(
                ObservarSesion(AuthRepoFalso()), ObservarObras(repo), CrearObra(repo),
                ActualizarObra(repo), CambiarEstadoObra(repo), EliminarObra(repo), ProgramarVisita(repo),
            )
            advanceUntilIdle()
            assertEquals(ListaObrasUiState.Error(ErrorObrasUi.RED), viewModel.state.value.lista)

            viewModel.reintentar()
            advanceUntilIdle()

            assertEquals(listOf("uid-1", "uid-1"), repo.usuariosConsultados)
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
    var deletedUserId: String? = null
    var deletedObraId: String? = null
    val visitas = mutableListOf<Pair<Instant?, Int?>>()
    var resultadoVisita: ResultadoObra<Unit> = ResultadoObra.Exito(Unit)
    var flujoObras: Flow<ResultadoObra<List<Obra>>>? = null
    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> {
        usuariosConsultados += userId
        return flujoObras ?: flowOf(ResultadoObra.Exito(obrasPorUsuario[userId].orEmpty()))
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

    override suspend fun programarVisita(
        userId: String, obraId: String, fechaUtc: Instant?, horaMinutos: Int?,
    ): ResultadoObra<Unit> {
        assertEquals("uid-1", userId)
        assertEquals("obra-1", obraId)
        visitas += fechaUtc to horaMinutos
        return resultadoVisita
    }

    override suspend fun eliminar(userId: String, obraId: String): ResultadoObra<Unit> {
        deletedUserId = userId
        deletedObraId = obraId
        return ResultadoObra.Exito(Unit)
    }
}
