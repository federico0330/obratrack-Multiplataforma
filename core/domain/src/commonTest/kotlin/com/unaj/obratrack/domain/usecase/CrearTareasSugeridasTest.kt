package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.repository.ErrorSugerir
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.RepositorioTareas
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import com.unaj.obratrack.domain.repository.ResultadoTarea
import com.unaj.obratrack.domain.repository.SugeridorDeTareas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Guarda lo que recibe y falla a partir de la llamada [fallarDesde] (empezando en 1), si se indica. */
private class TareasQueFallan(private val fallarDesde: Int? = null) : RepositorioTareas {
    private val tareas = mutableListOf<Tarea>()
    val titulos get() = tareas.map { it.titulo }
    private var llamadas = 0

    override suspend fun crear(userId: String, obraId: String, draft: TareaDraft): ResultadoTarea<Tarea> {
        llamadas++
        if (fallarDesde != null && llamadas >= fallarDesde) return ResultadoTarea.Fallo(ErrorTarea.RED)
        val tarea = Tarea("t$llamadas", obraId, userId, draft.titulo, categoria = draft.categoria)
        tareas += tarea
        return ResultadoTarea.Exito(tarea)
    }

    override fun observarDeObra(userId: String, obraId: String): Flow<ResultadoTarea<List<Tarea>>> =
        flowOf(ResultadoTarea.Exito(tareas.filter { it.userId == userId && it.obraId == obraId }))
    override suspend fun actualizar(userId: String, obraId: String, tareaId: String, draft: TareaDraft) = unsupported()
    override suspend fun cambiarEstado(userId: String, obraId: String, tareaId: String, estado: EstadoTarea) = unsupported()
    override suspend fun eliminar(userId: String, obraId: String, tareaId: String, vinculados: RelevamientosVinculados) =
        unsupported()

    private fun unsupported(): Nothing = error("no se usa")
}

class CrearTareasSugeridasTest {
    private val drafts = listOf(TareaDraft("Excavación"), TareaDraft("Fundaciones"), TareaDraft("Estructura"))

    @Test
    fun creaEnOrdenYOmiteTitulosAlRepetirLaSeleccion() = runTest {
        val repositorio = TareasQueFallan()

        val crear = CrearTareasSugeridas(CrearTarea(repositorio), ObservarTareas(repositorio))

        assertEquals(ResultadoCrearSugeridas.Exito(3), crear("u1", "o1", drafts))
        val segundaSeleccion = listOf(TareaDraft(" FUNDACIONES "), TareaDraft("Revoques"))
        assertEquals(ResultadoCrearSugeridas.Exito(1), crear("u1", "o1", segundaSeleccion))
        assertEquals(listOf("Excavación", "Fundaciones", "Estructura", "Revoques"), repositorio.titulos)
    }

    @Test
    fun sinTareasNoHaceNada() = runTest {
        val repositorio = TareasQueFallan()

        val resultado = CrearTareasSugeridas(CrearTarea(repositorio), ObservarTareas(repositorio))("u1", "o1", emptyList())

        assertEquals(ResultadoCrearSugeridas.Exito(0), resultado)
        assertEquals(emptyList(), repositorio.titulos)
    }

    @Test
    fun seDetieneEnElPrimerErrorEIndicaCuantasQuedaron() = runTest {
        val repositorio = TareasQueFallan(fallarDesde = 2)

        val resultado = CrearTareasSugeridas(CrearTarea(repositorio), ObservarTareas(repositorio))("u1", "o1", drafts)

        assertEquals(ResultadoCrearSugeridas.Fallo(creadas = 1, error = ErrorTarea.RED), resultado)
        assertEquals(listOf("Excavación"), repositorio.titulos)
    }

    @Test
    fun sinSesionNoCreaNada() = runTest {
        val repositorio = TareasQueFallan()

        val resultado = CrearTareasSugeridas(CrearTarea(repositorio), ObservarTareas(repositorio))("", "o1", drafts)

        assertEquals(ResultadoCrearSugeridas.Fallo(0, ErrorTarea.NO_AUTORIZADA), resultado)
        assertEquals(emptyList(), repositorio.titulos)
    }
}

class SugerirTareasTest {
    @Test
    fun devuelveLoQueProponeElSugeridor() = runTest {
        val sugeridor = object : SugeridorDeTareas {
            var tipoPedido: TipoObra? = null
            override suspend fun sugerir(tipo: TipoObra): ResultadoSugerir {
                tipoPedido = tipo
                return ResultadoSugerir.Exito(emptyList())
            }
        }

        val resultado = SugerirTareas(sugeridor)(TipoObra.BANO)

        assertEquals(ResultadoSugerir.Exito(emptyList()), resultado)
        assertEquals(TipoObra.BANO, sugeridor.tipoPedido)
    }

    @Test
    fun elFalloSePropagaSinLanzar() = runTest {
        val sugeridor = object : SugeridorDeTareas {
            override suspend fun sugerir(tipo: TipoObra) = ResultadoSugerir.Fallo(ErrorSugerir.SIN_CONEXION)
        }

        assertEquals(ResultadoSugerir.Fallo(ErrorSugerir.SIN_CONEXION), SugerirTareas(sugeridor)(TipoObra.COCINA))
    }
}
