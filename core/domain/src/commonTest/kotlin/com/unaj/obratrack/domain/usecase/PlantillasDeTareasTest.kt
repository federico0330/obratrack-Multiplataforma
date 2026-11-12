package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.PlantillasDeTareas
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TareaPlantilla
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.domain.model.aDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlantillasDeTareasTest {
    @Test
    fun todoTipoDeObraTieneTareasSugeridas() {
        TipoObra.entries.forEach { tipo ->
            assertTrue(PlantillasDeTareas.para(tipo).isNotEmpty(), "$tipo no tiene tareas")
        }
    }

    @Test
    fun ningunaPlantillaRepiteTareas() {
        TipoObra.entries.forEach { tipo ->
            val tareas = PlantillasDeTareas.para(tipo)
            assertEquals(tareas.size, tareas.toSet().size, "$tipo repite tareas")
        }
    }

    @Test
    fun todaTareaDelCatalogoSeUsaEnAlgunaPlantilla() {
        val usadas = TipoObra.entries.flatMap { PlantillasDeTareas.para(it) }.toSet()

        assertEquals(TareaPlantilla.entries.toSet(), usadas)
    }

    @Test
    fun laObraNuevaArrancaConLosPermisosYTerminaConLaEntrega() {
        val tareas = PlantillasDeTareas.para(TipoObra.CASA_NUEVA)

        assertEquals(TareaPlantilla.PLANOS_Y_PERMISOS, tareas.first())
        assertEquals(TareaPlantilla.RECEPCION_Y_ENTREGA, tareas.last())
    }

    @Test
    fun enUnaObraNuevaLaEstructuraVaDespuesDeLasFundaciones() {
        val tareas = PlantillasDeTareas.para(TipoObra.CASA_NUEVA)

        assertTrue(tareas.indexOf(TareaPlantilla.FUNDACIONES) < tareas.indexOf(TareaPlantilla.ESTRUCTURA_RESISTENTE))
        assertTrue(tareas.indexOf(TareaPlantilla.ESTRUCTURA_RESISTENTE) < tareas.indexOf(TareaPlantilla.PINTURA_INTERIOR))
    }

    @Test
    fun laDemolicionVaAntesQueLasInstalacionesEnUnaReforma() {
        val tareas = PlantillasDeTareas.para(TipoObra.REFORMA_INTEGRAL)

        assertTrue(tareas.indexOf(TareaPlantilla.DEMOLICION) < tareas.indexOf(TareaPlantilla.INSTALACION_ELECTRICA))
    }

    @Test
    fun laSugerenciaCreaUnaTareaConElTituloResueltoYSuCategoria() {
        val sugerencia = SugerenciaTarea(TituloSugerido.Plantilla(TareaPlantilla.INSTALACION_GAS), CategoriaTarea.GAS)

        val draft = sugerencia.aDraft("Instalación de gas")

        assertEquals("Instalación de gas", draft.titulo)
        assertEquals(CategoriaTarea.GAS, draft.categoria)
    }
}
