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
import com.unaj.obratrack.domain.usecase.ActualizarObra
import com.unaj.obratrack.domain.usecase.CambiarEstadoObra
import com.unaj.obratrack.domain.usecase.CrearObra
import com.unaj.obratrack.domain.usecase.EliminarObra
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/**
 * La baja de una obra con la lista en vivo: Firestore aplica el borrado en local y refresca la lista
 * antes de que el servidor confirme, así que la selección desaparece mientras la baja sigue en curso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BajaObraTest {
    private val obra = Obra(
        id = "obra-1",
        userId = "uid-1",
        nombre = "Escuela",
        cliente = "Municipio",
        direccion = "Calle 1",
        publica = false,
        estado = EstadoObra.ACTIVA,
        fechaInicio = Instant.parse("2026-11-01T00:00:00Z"),
    )

    private fun conViewModel(repo: RepoConListaEnVivo, bloque: suspend TestScope.(ObrasViewModel) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = ObrasViewModel(
                ObservarSesion(SesionDeLaBaja), ObservarObras(repo), CrearObra(repo), ActualizarObra(repo),
                CambiarEstadoObra(repo), EliminarObra(repo),
            )
            advanceUntilIdle()
            viewModel.abrirDetalle(obra)
            viewModel.solicitarEliminacion()
            viewModel.confirmarEliminacion()
            advanceUntilIdle()
            bloque(viewModel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun siLaListaRefrescaAntesDeTerminarLaBajaNoQuedaCargando() =
        conViewModel(RepoConListaEnVivo(obra, refrescaAntes = true)) { viewModel ->
            assertEquals(false, viewModel.state.value.eliminando)
            assertNull(viewModel.state.value.obraSeleccionada)
            assertNull(viewModel.state.value.errorOperacion)
        }

    @Test
    fun siLaListaNoRefrescaLaBajaCierraElDetalleYNoQuedaCargando() =
        conViewModel(RepoConListaEnVivo(obra, refrescaAntes = false)) { viewModel ->
            assertEquals(false, viewModel.state.value.eliminando)
            assertNull(viewModel.state.value.obraSeleccionada)
            assertNull(viewModel.state.value.errorOperacion)
        }

    @Test
    fun siFallaDespuesDeQuitarseDeLaListaApagaLaCargaYGuardaElError() =
        conViewModel(RepoConListaEnVivo(obra, refrescaAntes = true, resultado = ResultadoObra.Fallo(ErrorObra.RED))) { viewModel ->
            assertEquals(false, viewModel.state.value.eliminando)
            assertEquals(ErrorObrasUi.RED, viewModel.state.value.errorOperacion)
        }

    @Test
    fun siFallaSinQuitarseDeLaListaMantieneElDetalleConElError() =
        conViewModel(
            RepoConListaEnVivo(obra, refrescaAntes = false, resultado = ResultadoObra.Fallo(ErrorObra.FALLO_PARCIAL)),
        ) { viewModel ->
            assertEquals(false, viewModel.state.value.eliminando)
            assertEquals(ErrorObrasUi.BAJA_PARCIAL, viewModel.state.value.errorOperacion)
            assertEquals(obra, viewModel.state.value.obraSeleccionada)
        }
}

private val SesionDeLaBaja = object : RepositorioAutenticacion {
    override val usuarioActual: Flow<Usuario?> = MutableStateFlow(Usuario("uid-1", "a@example.com", null))
    override suspend fun iniciarSesion(email: String, clave: String) =
        ResultadoAcceso.Exito(Usuario("uid-1", "a@example.com", null))
    override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
    override suspend fun registrar(email: String, contrasena: String) =
        ResultadoRegistro.Exito(Usuario("uid-1", "a@example.com", null))
}

/** Con [refrescaAntes], simula el SDK: la lista deja de traer la obra antes de que la baja termine. */
private class RepoConListaEnVivo(
    private val obra: Obra,
    private val refrescaAntes: Boolean,
    private val resultado: ResultadoObra<Unit> = ResultadoObra.Exito(Unit),
) : RepositorioObras {
    private val lista = MutableStateFlow<ResultadoObra<List<Obra>>>(ResultadoObra.Exito(listOf(obra)))

    override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> = lista

    override suspend fun eliminar(userId: String, obraId: String): ResultadoObra<Unit> {
        if (refrescaAntes) {
            lista.value = ResultadoObra.Exito(emptyList())
            yield()
        }
        // Si la baja falla, el SDK revierte la escritura local y la obra vuelve a la lista.
        if (resultado is ResultadoObra.Fallo && refrescaAntes) lista.value = ResultadoObra.Exito(listOf(obra))
        return resultado
    }

    override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
        ResultadoObra.Fallo(ErrorObra.DESCONOCIDO)
    override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
    override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) = ResultadoObra.Exito(Unit)
}
