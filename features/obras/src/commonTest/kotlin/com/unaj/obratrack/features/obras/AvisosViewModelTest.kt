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
import com.unaj.obratrack.domain.repository.ProgramadorAvisos
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.SincronizarAvisosVisitas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AvisosViewModelTest {
    private val zona = TimeZone.of("America/Argentina/Buenos_Aires")
    private val ahora = Instant.parse("2026-11-10T01:30:00Z")

    @Test
    fun programaLosAvisosDeLasVisitasFuturasDeLaSesionYNoLosDeLasVencidas() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoAvisos()
            repo.obrasPorUsuario["uid-1"] = listOf(
                obra("futura", "2027-01-05", 10 * 60),
                obra("sinHora", "2027-01-06"),
                obra("vencida", "2020-01-01"),
                obra("sinFecha", null),
            )
            val puerto = PuertoFalso()
            val viewModel = AvisosViewModel(
                ObservarSesion(AuthRepoFalsoAvisos()), ObservarObras(repo),
                SincronizarAvisosVisitas(puerto), clock = object : Clock { override fun now(): Instant = ahora }, zona = zona,
            )
            advanceUntilIdle()

            assertEquals(setOf("futura", "sinHora"), puerto.programaciones.keys)
            assertEquals(Instant.parse("2027-01-05T13:00:00Z"), puerto.programaciones.getValue("futura"))
            assertTrue(puerto.cancelaciones.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun alCambiarLaSesionActualizaLosAvisosDeLaNuevaCuenta() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = ObrasRepoAvisos()
            repo.obrasPorUsuario["uid-1"] = listOf(obra("deUno", "2027-01-05"))
            repo.obrasPorUsuario["uid-2"] = listOf(obra("deOtro", "2027-01-05"))
            val sesion = AuthRepoFalsoAvisos()
            val puerto = PuertoFalso()
            val viewModel = AvisosViewModel(
                ObservarSesion(sesion), ObservarObras(repo),
                SincronizarAvisosVisitas(puerto), clock = object : Clock { override fun now(): Instant = ahora }, zona = zona,
            )
            advanceUntilIdle()
            assertEquals(setOf("deUno"), puerto.programaciones.keys)

            sesion.usuarioActual.value = Usuario("uid-2", "otra@example.com", null)
            advanceUntilIdle()
            assertEquals(setOf("deOtro"), puerto.programaciones.keys)
            assertEquals(listOf("deUno"), puerto.cancelaciones.distinct())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun obra(id: String, fecha: String?, hora: Int? = null) = Obra(
        id, "uid-1", id, "Cliente", "Dirección", false, EstadoObra.ACTIVA,
        Instant.parse("2026-11-01T00:00:00Z"),
        proximaVisita = fecha?.let { Instant.parse("${it}T00:00:00Z") },
        horaVisita = hora,
    )

    private class AuthRepoFalsoAvisos : RepositorioAutenticacion {
        override val usuarioActual = MutableStateFlow<Usuario?>(Usuario("uid-1", "fede@example.com", null))
        override suspend fun iniciarSesion(email: String, clave: String) = ResultadoAcceso.Exito(usuarioActual.value!!)
        override suspend fun cerrarSesion() = ResultadoCerrarSesion.Exito
        override suspend fun registrar(email: String, contrasena: String) = ResultadoRegistro.Exito(usuarioActual.value!!)
    }

    private class ObrasRepoAvisos : RepositorioObras {
        val obrasPorUsuario = mutableMapOf<String, List<Obra>>()
        override fun observarPropias(userId: String): Flow<ResultadoObra<List<Obra>>> =
            flowOf(ResultadoObra.Exito(obrasPorUsuario[userId].orEmpty()))
        override suspend fun crear(userId: String, draft: ObraDraft, estado: EstadoObra) =
            ResultadoObra.Exito(Obra("nueva", userId, draft.nombre, draft.cliente, draft.direccion, draft.publica, estado, draft.fechaInicio))
        override suspend fun actualizar(userId: String, obraId: String, draft: ObraDraft) = ResultadoObra.Exito(Unit)
        override suspend fun cambiarEstado(userId: String, obraId: String, estado: EstadoObra) = ResultadoObra.Exito(Unit)
        override suspend fun programarVisita(userId: String, obraId: String, fechaUtc: Instant?, horaMinutos: Int?) =
            ResultadoObra.Exito(Unit)
        override suspend fun eliminar(userId: String, obraId: String) = ResultadoObra.Exito(Unit)
    }

    private class PuertoFalso : ProgramadorAvisos {
        val programaciones = mutableMapOf<String, Instant>()
        val cancelaciones = mutableListOf<String>()
        override suspend fun programados(): Map<String, Instant> = programaciones.toMap()
        override suspend fun programar(aviso: com.unaj.obratrack.domain.model.AvisoVisita) {
            programaciones[aviso.obraId] = aviso.disparo
        }
        override suspend fun cancelar(obraId: String) {
            programaciones.remove(obraId)
            cancelaciones.add(obraId)
        }
    }
}
