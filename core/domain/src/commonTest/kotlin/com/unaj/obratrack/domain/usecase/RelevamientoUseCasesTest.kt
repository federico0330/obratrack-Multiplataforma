package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Relevamiento
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
        val crear = CrearRelevamiento(repo)
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
            assertEquals(ResultadoRelevamiento.Fallo(error), CrearRelevamiento(repo)("uid", "obra", draft))
            assertNull(repo.creado)
        }
    }

    @Test
    fun sinUsuarioUObraNoCreaNiConsulta() = runTest {
        for ((uid, obra) in listOf("" to "obra", "uid" to " ")) {
            val repo = RelevamientosFalso()
            val error = ResultadoRelevamiento.Fallo(ErrorRelevamiento.NO_AUTORIZADA)
            assertEquals(error, CrearRelevamiento(repo)(uid, obra, RelevamientoDraft(Instant.fromEpochMilliseconds(10), "Nota")))
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
        assertEquals(error, CrearRelevamiento(repo)("uid", "obra", RelevamientoDraft(Instant.fromEpochMilliseconds(10), "Nota")))
    }
}

private class RelevamientosFalso : RepositorioRelevamientos {
    var lista: ResultadoRelevamiento<List<Relevamiento>> = ResultadoRelevamiento.Exito(emptyList())
    var alta: ResultadoRelevamiento<Relevamiento> = ResultadoRelevamiento.Fallo(ErrorRelevamiento.RED)
    var creado: RelevamientoDraft? = null
    var destino: Pair<String, String>? = null
    override fun observarDeObra(userId: String, obraId: String) = flowOf(lista).also { destino = userId to obraId }
    override suspend fun crear(userId: String, obraId: String, draft: RelevamientoDraft): ResultadoRelevamiento<Relevamiento> {
        destino = userId to obraId
        creado = draft
        return alta
    }
}
