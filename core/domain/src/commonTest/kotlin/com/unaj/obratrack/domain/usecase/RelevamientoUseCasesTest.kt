package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.CambioFoto
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.domain.model.FiltroRelevamientos
import com.unaj.obratrack.domain.repository.AlmacenFotos
import com.unaj.obratrack.domain.repository.ResultadoSubidaFoto
import kotlin.test.assertNotEquals
import com.unaj.obratrack.domain.model.filtrar
import com.unaj.obratrack.domain.model.RelevamientoDraft
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.RepositorioRelevamientos
import com.unaj.obratrack.domain.repository.ResultadoRelevamiento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RelevamientoUseCasesTest {
    @Test
    fun crearNormalizaLaNotaYConservaElVinculoOpcional() = runTest {
        val repo = RelevamientosFalso()
        val crear = CrearRelevamiento(repo, AlmacenFotosFalso())
        for (tarea in listOf(null, "tarea-1")) {
            crear("uid", "obra", RelevamientoDraft(Instant.fromEpochMilliseconds(10), "  Avance  ", tarea))
            assertEquals("Avance", repo.creado?.nota)
            assertEquals(tarea, repo.creado?.tareaId)
            assertEquals("uid" to "obra", repo.destino)
        }
    }

    @Test
    fun entradasInvalidasNoLleganAlRepositorio() = runTest {
        val fecha = Instant.fromEpochMilliseconds(10)
        val casos = listOf(
            RelevamientoDraft(fecha, " ") to ErrorRelevamiento.NOTA_VACIA,
            RelevamientoDraft(Instant.fromEpochMilliseconds(0), "Nota") to ErrorRelevamiento.FECHA_INVALIDA,
            RelevamientoDraft(fecha, "Nota", " ") to ErrorRelevamiento.TAREA_NO_DISPONIBLE,
        )
        for ((draft, error) in casos) {
            val repo = RelevamientosFalso()
            assertEquals(ResultadoRelevamiento.Fallo(error), CrearRelevamiento(repo, AlmacenFotosFalso())("uid", "obra", draft))
            assertNull(repo.creado)
        }
    }

    @Test
    fun sinUsuarioUObraNoCreaNiConsulta() = runTest {
        for ((uid, obra) in listOf("" to "obra", "uid" to " ")) {
            val repo = RelevamientosFalso()
            val error = ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_AUTORIZADA)
            assertEquals(error, CrearRelevamiento(repo, AlmacenFotosFalso())(uid, obra, RelevamientoDraft(Instant.fromEpochMilliseconds(10), "Nota")))
            assertEquals(error, ObservarRelevamientos(repo)(uid, obra).first())
            assertNull(repo.destino)
        }
    }

    @Test
    fun listadoOrdenaPorFechaDescendenteYDesempataPorId() = runTest {
        val repo = RelevamientosFalso()
        fun item(id: String, millis: Long) = Relevamiento(id, "obra", "uid", Instant.fromEpochMilliseconds(millis), id)
        repo.lista = ResultadoRelevamiento.Exito(listOf(item("z", 10), item("b", 30), item("a", 30)))
        val result = ObservarRelevamientos(repo)("uid", "obra").first() as ResultadoRelevamiento.Exito
        assertEquals(listOf("a", "b", "z"), result.dato.map { it.id })
    }

    @Test
    fun conservaErroresDeLecturaYAlta() = runTest {
        val repo = RelevamientosFalso()
        val error = ResultadoRelevamiento.Fallo(ErrorRelevamiento.RED)
        repo.lista = error
        repo.alta = error
        assertEquals(error, ObservarRelevamientos(repo)("uid", "obra").first())
        assertEquals(error, CrearRelevamiento(repo, AlmacenFotosFalso())("uid", "obra", RelevamientoDraft(Instant.fromEpochMilliseconds(10), "Nota")))
    }
    @Test
    fun editarValidaNormalizaYConservaIdYDesvinculo() = runTest {
        val repo = RelevamientosFalso()
        val draft = RelevamientoDraft(Instant.fromEpochMilliseconds(20), "  Nota editada  ")
        assertEquals(ResultadoRelevamiento.Exito(Unit), ActualizarRelevamiento(repo, AlmacenFotosFalso())("uid", "obra", "id", draft))
        assertEquals("id", repo.modificado)
        assertEquals("Nota editada", repo.creado?.nota)
        assertNull(repo.creado?.tareaId)
        assertEquals(ResultadoRelevamiento.Fallo(ErrorRelevamiento.NOTA_VACIA),
            ActualizarRelevamiento(repo, AlmacenFotosFalso())("uid", "obra", "id", draft.copy(nota = " ")))
    }

    @Test
    fun idsInvalidosNoInvocanLaBaja() = runTest {
        val repo = RelevamientosFalso()
        assertEquals(ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_AUTORIZADA), EliminarRelevamiento(repo)("", "obra", "id"))
        assertEquals(ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_ENCONTRADO), EliminarRelevamiento(repo)("uid", "obra", " "))
        assertNull(repo.modificado)
        assertEquals(ResultadoRelevamiento.Exito(Unit), EliminarRelevamiento(repo)("uid", "obra", "id"))
        assertEquals("id", repo.modificado)
    }

    @Test
    fun filtrosDistinguenTodosSinTareaYTareaElegida() {
        val items = listOf(null, "a", "b").mapIndexed { i, tarea ->
            Relevamiento(i.toString(), "obra", "uid", Instant.fromEpochMilliseconds(20), "Nota", tarea)
        }
        assertEquals(items, items.filtrar(FiltroRelevamientos.Todos))
        assertEquals(listOf("0"), items.filtrar(FiltroRelevamientos.SinTarea).map { it.id })
        assertEquals(listOf("1"), items.filtrar(FiltroRelevamientos.DeTarea("a")).map { it.id })
    }

    @Test
    fun crearSubeLaFotoAntesDeEscribirYGuardaSuUrl() = runTest {
        val eventos = mutableListOf<String>()
        val repo = RelevamientosFalso(eventos)
        val fotos = AlmacenFotosFalso(eventos)
        val bytes = byteArrayOf(1, 2, 3)
        CrearRelevamiento(repo, fotos) { "nombre-unico" }("uid", "obra", RelevamientoDraft(FECHA, "Nota", cambioFoto = CambioFoto.Nueva(bytes)))
        assertEquals(listOf("subir", "crear"), eventos)
        assertEquals(Triple(bytes.toList(), "relevamientos/obra", "nombre-unico"), fotos.subida)
        assertEquals("https://foto", repo.fotoUrl)
        assertEquals(CambioFoto.SinCambio, repo.creado?.cambioFoto)
    }

    @Test
    fun editarSubeLaFotoAntesDeEscribirYPasaLaUrlNueva() = runTest {
        val eventos = mutableListOf<String>()
        val repo = RelevamientosFalso(eventos)
        ActualizarRelevamiento(repo, AlmacenFotosFalso(eventos))("uid", "obra", "id",
            RelevamientoDraft(FECHA, "Nota", cambioFoto = CambioFoto.Nueva(byteArrayOf(1))))
        assertEquals(listOf("subir", "actualizar"), eventos)
        assertEquals(CambioFotoUrl.Nueva("https://foto"), repo.cambioFotoUrl)
    }

    @Test
    fun cadaFalloDeSubidaSeTraduceYNoEscribe() = runTest {
        val casos = listOf(
            ResultadoSubidaFoto.SinConexion to ErrorRelevamiento.FOTO_SIN_CONEXION,
            ResultadoSubidaFoto.MuyGrande to ErrorRelevamiento.FOTO_MUY_GRANDE,
            ResultadoSubidaFoto.Fallo to ErrorRelevamiento.FOTO_FALLO,
        )
        for ((subida, error) in casos) {
            val repo = RelevamientosFalso()
            val fotos = AlmacenFotosFalso(resultado = subida)
            val draft = RelevamientoDraft(FECHA, "Nota", cambioFoto = CambioFoto.Nueva(byteArrayOf(1)))
            assertEquals(ResultadoRelevamiento.Fallo(error), CrearRelevamiento(repo, fotos)("uid", "obra", draft))
            assertEquals(ResultadoRelevamiento.Fallo(error), ActualizarRelevamiento(repo, fotos)("uid", "obra", "id", draft))
            assertNull(repo.creado)
            assertNull(repo.modificado)
        }
    }

    @Test
    fun sinCambioYQuitarNoSuben() = runTest {
        val repo = RelevamientosFalso()
        val fotos = AlmacenFotosFalso()
        val actualizar = ActualizarRelevamiento(repo, fotos)
        actualizar("uid", "obra", "id", RelevamientoDraft(FECHA, "Nota"))
        assertEquals(CambioFotoUrl.SinCambio, repo.cambioFotoUrl)
        actualizar("uid", "obra", "id", RelevamientoDraft(FECHA, "Nota", cambioFoto = CambioFoto.Quitar))
        assertEquals(CambioFotoUrl.Quitar, repo.cambioFotoUrl)
        CrearRelevamiento(repo, fotos)("uid", "obra", RelevamientoDraft(FECHA, "Nota", cambioFoto = CambioFoto.Quitar))
        assertNull(repo.fotoUrl)
        assertNull(fotos.subida)
    }

    @Test
    fun unaEntradaInvalidaNoSubeLaFoto() = runTest {
        val fotos = AlmacenFotosFalso()
        val draft = RelevamientoDraft(FECHA, " ", cambioFoto = CambioFoto.Nueva(byteArrayOf(1)))
        assertEquals(ResultadoRelevamiento.Fallo(ErrorRelevamiento.NOTA_VACIA), CrearRelevamiento(RelevamientosFalso(), fotos)("uid", "obra", draft))
        assertNull(fotos.subida)
    }

    @Test
    fun elNombrePorDefectoEsDistintoEnCadaSubida() {
        assertNotEquals(nombreFotoAleatorio(), nombreFotoAleatorio())
    }
}

private val FECHA = Instant.fromEpochMilliseconds(10)

private class AlmacenFotosFalso(
    private val eventos: MutableList<String> = mutableListOf(),
    private val resultado: ResultadoSubidaFoto = ResultadoSubidaFoto.Exito("https://foto"),
) : AlmacenFotos {
    var subida: Triple<List<Byte>, String, String>? = null
    override suspend fun subir(bytes: ByteArray, carpeta: String, nombre: String): ResultadoSubidaFoto {
        eventos += "subir"
        subida = Triple(bytes.toList(), carpeta, nombre)
        return resultado
    }
}

private class RelevamientosFalso(private val eventos: MutableList<String> = mutableListOf()) : RepositorioRelevamientos {
    var lista: ResultadoRelevamiento<List<Relevamiento>> = ResultadoRelevamiento.Exito(emptyList())
    var alta: ResultadoRelevamiento<Relevamiento> = ResultadoRelevamiento.Fallo(ErrorRelevamiento.RED)
    var modificado: String? = null
    var creado: RelevamientoDraft? = null
    var destino: Pair<String, String>? = null
    var fotoUrl: String? = null
    var cambioFotoUrl: CambioFotoUrl? = null
    override fun observarDeObra(userId: String, obraId: String) = flowOf(lista).also { destino = userId to obraId }
    override suspend fun crear(userId: String, obraId: String, draft: RelevamientoDraft, fotoUrl: String?): ResultadoRelevamiento<Relevamiento> {
        eventos += "crear"
        destino = userId to obraId
        creado = draft
        this.fotoUrl = fotoUrl
        return alta
    }
    override suspend fun actualizar(
        userId: String, obraId: String, id: String, draft: RelevamientoDraft, foto: CambioFotoUrl,
    ): ResultadoRelevamiento<Unit> {
        eventos += "actualizar"
        modificado = id
        creado = draft
        cambioFotoUrl = foto
        return ResultadoRelevamiento.Exito(Unit)
    }
    override suspend fun eliminar(userId: String, obraId: String, id: String): ResultadoRelevamiento<Unit> {
        modificado = id
        return ResultadoRelevamiento.Exito(Unit)
    }
}
