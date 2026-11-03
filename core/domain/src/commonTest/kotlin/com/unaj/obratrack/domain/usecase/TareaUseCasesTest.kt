package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.EstadoTarea
import com.unaj.obratrack.domain.model.RelevamientosVinculados
import com.unaj.obratrack.domain.model.Tarea
import com.unaj.obratrack.domain.model.TareaDraft
import com.unaj.obratrack.domain.repository.ErrorTarea
import com.unaj.obratrack.domain.repository.ResultadoTarea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TareaUseCasesTest {
    private val repositorio = RepositorioTareasFalso()

    @Test
    fun crearNormalizaYEnviaAlRepositorio() = runTest {
        val resultado = CrearTarea(repositorio)("uid-1", "obra-1", TareaDraft(titulo = "  Colar losa ", descripcion = " x "))

        val tarea = assertIs<ResultadoTarea.Exito<Tarea>>(resultado).dato
        assertEquals("Colar losa", tarea.titulo)
        assertEquals("x", repositorio.draft?.descripcion)
        assertEquals("obra-1", repositorio.obraId)
    }

    @Test
    fun crearRechazaTituloVacioSinLlamarAlRepositorio() = runTest {
        val resultado = CrearTarea(repositorio)("uid-1", "obra-1", TareaDraft(titulo = "  "))

        assertEquals(ResultadoTarea.Fallo(ErrorTarea.TITULO_VACIO), resultado)
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun crearRechazaFechasIncompletasYFinAnterior() = runTest {
        val dia = LocalDate(2026, 11, 10)
        val incompletas = CrearTarea(repositorio)("uid-1", "obra-1", TareaDraft("T", fechaInicio = dia))
        val invertidas = CrearTarea(repositorio)(
            "uid-1",
            "obra-1",
            TareaDraft("T", fechaInicio = dia, fechaFin = LocalDate(2026, 11, 9)),
        )

        assertEquals(ResultadoTarea.Fallo(ErrorTarea.FECHAS_INCOMPLETAS), incompletas)
        assertEquals(ResultadoTarea.Fallo(ErrorTarea.FECHA_FIN_ANTERIOR), invertidas)
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun crearRechazaUsuarioOObraVacios() = runTest {
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            CrearTarea(repositorio)(" ", "obra-1", TareaDraft("T")),
        )
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            CrearTarea(repositorio)("uid-1", "", TareaDraft("T")),
        )
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun actualizarValidaYEnviaIdentificadores() = runTest {
        val resultado = ActualizarTarea(repositorio)("uid-1", "obra-1", "tarea-5", TareaDraft(" Pintar "))

        assertEquals(ResultadoTarea.Exito(Unit), resultado)
        assertEquals("tarea-5", repositorio.tareaId)
        assertEquals("Pintar", repositorio.draft?.titulo)
    }

    @Test
    fun actualizarRechazaTituloVacioYTareaVacia() = runTest {
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.TITULO_VACIO),
            ActualizarTarea(repositorio)("uid-1", "obra-1", "tarea-5", TareaDraft("")),
        )
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            ActualizarTarea(repositorio)("uid-1", "obra-1", " ", TareaDraft("T")),
        )
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun cambiarEstadoEnviaElEstado() = runTest {
        val resultado = CambiarEstadoTarea(repositorio)("uid-1", "obra-1", "tarea-5", EstadoTarea.COMPLETADA)

        assertEquals(ResultadoTarea.Exito(Unit), resultado)
        assertEquals(EstadoTarea.COMPLETADA, repositorio.estado)
    }

    @Test
    fun cambiarEstadoRechazaIdsVacios() = runTest {
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            CambiarEstadoTarea(repositorio)("uid-1", "obra-1", "", EstadoTarea.EN_CURSO),
        )
        assertEquals(0, repositorio.llamadas)
    }

    @Test
    fun eliminarDesvinculaPorDefectoYPropagaLaEleccion() = runTest {
        EliminarTarea(repositorio)("uid-1", "obra-1", "tarea-5")
        assertEquals(RelevamientosVinculados.DESVINCULAR, repositorio.vinculados)

        EliminarTarea(repositorio)("uid-1", "obra-1", "tarea-5", RelevamientosVinculados.ELIMINAR)
        assertEquals(RelevamientosVinculados.ELIMINAR, repositorio.vinculados)
    }

    @Test
    fun eliminarPropagaFalloParcialYRechazaIdsVacios() = runTest {
        repositorio.resultadoUnit = ResultadoTarea.Fallo(ErrorTarea.FALLO_PARCIAL)
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.FALLO_PARCIAL),
            EliminarTarea(repositorio)("uid-1", "obra-1", "tarea-5"),
        )

        val antes = repositorio.llamadas
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            EliminarTarea(repositorio)("", "obra-1", "tarea-5"),
        )
        assertEquals(antes, repositorio.llamadas)
    }

    @Test
    fun observarDevuelveLasTareasOrdenadas() = runTest {
        val sinFecha = Tarea("1", "obra-1", "uid-1", "Zócalos")
        val temprana = Tarea("2", "obra-1", "uid-1", "Cimientos", fechaInicio = LocalDate(2026, 11, 1), fechaFin = LocalDate(2026, 11, 5))
        val tardia = Tarea("3", "obra-1", "uid-1", "Pintura", fechaInicio = LocalDate(2026, 12, 1), fechaFin = LocalDate(2026, 12, 9))
        repositorio.tareas = ResultadoTarea.Exito(listOf(sinFecha, tardia, temprana))

        val resultado = ObservarTareas(repositorio)("uid-1", "obra-1").first()

        assertEquals(ResultadoTarea.Exito(listOf(temprana, tardia, sinFecha)), resultado)
    }

    @Test
    fun observarPropagaFallosYRechazaIdsVacios() = runTest {
        repositorio.tareas = ResultadoTarea.Fallo(ErrorTarea.RED)

        assertEquals(ResultadoTarea.Fallo(ErrorTarea.RED), ObservarTareas(repositorio)("uid-1", "obra-1").first())
        assertEquals(
            ResultadoTarea.Fallo(ErrorTarea.NO_AUTORIZADA),
            ObservarTareas(repositorio)("", "obra-1").first(),
        )
    }
}
