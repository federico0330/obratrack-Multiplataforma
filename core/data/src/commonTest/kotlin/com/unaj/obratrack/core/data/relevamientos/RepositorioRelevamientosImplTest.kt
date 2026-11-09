package com.unaj.obratrack.core.data.relevamientos

import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoDataSource
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoFailure
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoResult
import com.unaj.obratrack.core.firebase.relevamientos.FotoUpdate
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoUpdateFields
import com.unaj.obratrack.core.firebase.relevamientos.UbicacionUpdate
import com.unaj.obratrack.domain.model.CambioUbicacion
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class RepositorioRelevamientosImplTest {
    @Test
    fun leeDocumentosNativosConYsinCamposOpcionales() = runTest {
        val remote = RemotoFalso()
        val completo = remote.dto.copy(tareaId = "tarea", fotoUrl = "https://example.com/foto.jpg", latitud = -34.0,
            longitud = -58.0, proximaVisitaMillis = 40, horaVisita = 600)
        remote.lista = FirebaseRelevamientoResult.Success(listOf(remote.dto, completo))
        val result = RepositorioRelevamientosImpl(remote).observarDeObra("uid", "obra").first() as ResultadoRelevamiento.Exito
        assertEquals("uid" to "obra", remote.destino)
        assertNull(result.dato.first().tareaId)
        assertEquals(Ubicacion(-34.0, -58.0), result.dato.last().ubicacion)
        assertEquals("tarea", result.dato.last().tareaId)
        assertEquals(Instant.fromEpochMilliseconds(40), result.dato.last().proximaVisita)
        assertEquals(600, result.dato.last().horaVisita)
    }

    @Test
    fun crearEnviaUidObraFechaYVinculoYRecuperaIdRemoto() = runTest {
        val remote = RemotoFalso()
        val result = RepositorioRelevamientosImpl(remote).crear("uid", "obra",
            RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota", "tarea")) as ResultadoRelevamiento.Exito
        assertEquals("remoto", result.dato.id)
        assertEquals(RelevamientoDto("", "obra", "uid", 20, "Nota", "tarea"), remote.enviado)
    }

    @Test
    fun crearYEditarPasanLaFotoYaSubida() = runTest {
        val remote = RemotoFalso()
        val repo = RepositorioRelevamientosImpl(remote)
        val draft = RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")
        repo.crear("uid", "obra", draft, "https://foto")
        assertEquals("https://foto", remote.enviado?.fotoUrl)
        val casos = listOf(
            CambioFotoUrl.SinCambio to FotoUpdate.SinCambio,
            CambioFotoUrl.Nueva("https://otra") to FotoUpdate.Nueva("https://otra"),
            CambioFotoUrl.Quitar to FotoUpdate.Quitar,
        )
        for ((cambio, esperado) in casos) {
            repo.actualizar("uid", "obra", "id", draft, cambio)
            assertEquals(esperado, remote.editado?.foto)
        }
    }

    @Test
    fun crearEscribeLaUbicacionSoloSiEsNueva() = runTest {
        val remote = RemotoFalso()
        val repo = RepositorioRelevamientosImpl(remote)
        val base = RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")
        repo.crear("uid", "obra", base.copy(cambioUbicacion = CambioUbicacion.Nueva(Ubicacion(-34.7746, -58.2672))))
        assertEquals(-34.7746, remote.enviado?.latitud)
        assertEquals(-58.2672, remote.enviado?.longitud)
        // Al crear no hay ubicación previa: SinCambio y Quitar equivalen a no tenerla.
        for (cambio in listOf(CambioUbicacion.SinCambio, CambioUbicacion.Quitar)) {
            repo.crear("uid", "obra", base.copy(cambioUbicacion = cambio))
            assertNull(remote.enviado?.latitud)
            assertNull(remote.enviado?.longitud)
        }
    }

    @Test
    fun editarPasaElCambioDeUbicacionYSinCambioNoLaToca() = runTest {
        val remote = RemotoFalso()
        val repo = RepositorioRelevamientosImpl(remote)
        val base = RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")
        val casos = listOf(
            CambioUbicacion.SinCambio to UbicacionUpdate.SinCambio,
            CambioUbicacion.Nueva(Ubicacion(-34.5, -58.5)) to UbicacionUpdate.Nueva(-34.5, -58.5),
            CambioUbicacion.Quitar to UbicacionUpdate.Quitar,
        )
        for ((cambio, esperado) in casos) {
            repo.actualizar("uid", "obra", "id", base.copy(cambioUbicacion = cambio))
            assertEquals(esperado, remote.editado?.ubicacion)
        }
        repo.actualizar("uid", "obra", "id", base)
        assertEquals(UbicacionUpdate.SinCambio, remote.editado?.ubicacion)
    }

    @Test
    fun laFotoDeCloudinarySeMuestraReducidaYLaOriginalSeConserva() = runTest {
        val remote = RemotoFalso()
        val original = "https://res.cloudinary.com/iwsier4h/image/upload/v1/relevamientos/obra/a.jpg"
        remote.lista = FirebaseRelevamientoResult.Success(listOf(remote.dto.copy(fotoUrl = original)))
        val item = (RepositorioRelevamientosImpl(remote).observarDeObra("uid", "obra").first() as ResultadoRelevamiento.Exito).dato.single()
        assertEquals(original, item.fotoUrl)
        assertEquals("https://res.cloudinary.com/iwsier4h/image/upload/c_fill,w_800/v1/relevamientos/obra/a.jpg", item.fotoVistaUrl)
        assertEquals("https://res.cloudinary.com/iwsier4h/image/upload/c_fill,w_160,h_160/v1/relevamientos/obra/a.jpg", item.fotoMiniaturaUrl)
    }

    @Test
    fun traduceCadaFalloRemotoSinMostrarExcepciones() = runTest {
        for ((remoteError, error) in listOf(
            FirebaseRelevamientoFailure.PermissionDenied to ErrorRelevamiento.NO_AUTORIZADA,
            FirebaseRelevamientoFailure.NotFound to ErrorRelevamiento.NO_ENCONTRADO,
            FirebaseRelevamientoFailure.TaskUnavailable to ErrorRelevamiento.TAREA_NO_DISPONIBLE,
            FirebaseRelevamientoFailure.Network to ErrorRelevamiento.RED,
            FirebaseRelevamientoFailure.Unexpected to ErrorRelevamiento.DESCONOCIDO,
        )) {
            val remote = RemotoFalso().apply { fallo = remoteError }
            val repo = RepositorioRelevamientosImpl(remote)
            val esperado = ResultadoRelevamiento.Fallo(error)
            assertEquals(esperado, repo.crear("uid", "obra", RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")))
            assertEquals(esperado, repo.actualizar("uid", "obra", "id", RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")))
            assertEquals(esperado, repo.eliminar("uid", "obra", "id"))
            assertEquals(esperado, repo.observarDeObra("uid", "obra").first())
        }
    }

    @Test
    fun excepcionInesperadaSeInformaComoFallo() = runTest {
        val repo = RepositorioRelevamientosImpl(RemotoFalso().apply { excepcion = IllegalStateException() })
        assertEquals(ResultadoRelevamiento.Fallo(ErrorRelevamiento.DESCONOCIDO), repo.observarDeObra("uid", "obra").first())
    }

    @Test
    fun cancelacionSePropagaEnLecturaYAlta() = runTest {
        val repo = RepositorioRelevamientosImpl(RemotoFalso().apply { excepcion = CancellationException() })
        assertFailsWith<CancellationException> { repo.observarDeObra("uid", "obra").first() }
        assertFailsWith<CancellationException> { repo.crear("uid", "obra", RelevamientoDraft(Instant.fromEpochMilliseconds(20), "Nota")) }
    }
    @Test
    fun editarMapeaSoloLosCamposEditablesYBajaConservaElDestino() = runTest {
        val remote = RemotoFalso()
        val repo = RepositorioRelevamientosImpl(remote)
        assertEquals(ResultadoRelevamiento.Exito(Unit), repo.actualizar("uid", "obra", "id",
            RelevamientoDraft(Instant.fromEpochMilliseconds(40), "Nota nueva", null)))
        assertEquals(RelevamientoUpdateFields(40, "Nota nueva", null), remote.editado)
        assertEquals(ResultadoRelevamiento.Exito(Unit), repo.eliminar("uid", "obra", "id"))
        assertEquals(Triple("uid", "obra", "id"), remote.operacion)
    }

}

private class RemotoFalso : FirebaseRelevamientoDataSource {
    val dto = RelevamientoDto("remoto", "obra", "uid", 20, "Nota")
    var lista: FirebaseRelevamientoResult<List<RelevamientoDto>> = FirebaseRelevamientoResult.Success(emptyList())
    var destino: Pair<String, String>? = null
    var enviado: RelevamientoDto? = null
    var editado: RelevamientoUpdateFields? = null
    var operacion: Triple<String, String, String>? = null
    var fallo: FirebaseRelevamientoFailure? = null
    var excepcion: Exception? = null
    override fun observarDeObra(userId: String, obraId: String) = flow {
        destino = userId to obraId
        excepcion?.let { throw it }
        emit(fallo?.let { FirebaseRelevamientoResult.Failure(it) } ?: lista)
    }
    override suspend fun crear(dto: RelevamientoDto): FirebaseRelevamientoResult<RelevamientoDto> {
        excepcion?.let { throw it }
        enviado = dto
        return fallo?.let { FirebaseRelevamientoResult.Failure(it) } ?: FirebaseRelevamientoResult.Success(this.dto)
    }
    override suspend fun actualizar(userId: String, obraId: String, id: String, fields: RelevamientoUpdateFields): FirebaseRelevamientoResult<Unit> {
        editado = fields
        return eliminar(userId, obraId, id)
    }
    override suspend fun eliminar(userId: String, obraId: String, id: String): FirebaseRelevamientoResult<Unit> {
        excepcion?.let { throw it }
        operacion = Triple(userId, obraId, id)
        return fallo?.let { FirebaseRelevamientoResult.Failure(it) } ?: FirebaseRelevamientoResult.Success(Unit)
    }
}
