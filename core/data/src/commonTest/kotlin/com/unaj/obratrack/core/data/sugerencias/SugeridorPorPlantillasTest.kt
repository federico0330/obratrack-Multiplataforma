package com.unaj.obratrack.core.data.sugerencias

import com.unaj.obratrack.domain.model.CategoriaTarea
import com.unaj.obratrack.domain.model.PlantillasDeTareas
import com.unaj.obratrack.domain.model.TareaPlantilla
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.domain.repository.ResultadoSugerir
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SugeridorPorPlantillasTest {
    @Test
    fun devuelveLasTareasDeLaPlantillaEnOrdenYConSuCategoria() = runTest {
        val resultado = SugeridorPorPlantillas().sugerir(TipoObra.PINTURA)

        val sugerencias = assertIs<ResultadoSugerir.Exito>(resultado).sugerencias
        assertEquals(PlantillasDeTareas.para(TipoObra.PINTURA), sugerencias.map { (it.titulo as TituloSugerido.Plantilla).tarea })
        assertEquals(
            listOf(CategoriaTarea.ALBANILERIA, CategoriaTarea.PINTURA, CategoriaTarea.PINTURA, CategoriaTarea.LIMPIEZA),
            sugerencias.map { it.categoria },
        )
    }

    @Test
    fun todoTipoDeObraDevuelveExito() = runTest {
        TipoObra.entries.forEach { tipo ->
            val resultado = SugeridorPorPlantillas().sugerir(tipo)

            assertEquals(PlantillasDeTareas.para(tipo).size, assertIs<ResultadoSugerir.Exito>(resultado).sugerencias.size)
        }
    }

    @Test
    fun laCategoriaDeLaSugerenciaEsLaDeLaTareaDelCatalogo() = runTest {
        val resultado = SugeridorPorPlantillas().sugerir(TipoObra.CASA_NUEVA)

        assertIs<ResultadoSugerir.Exito>(resultado).sugerencias.forEach {
            assertEquals((it.titulo as TituloSugerido.Plantilla).tarea.categoria, it.categoria)
        }
        assertEquals(TareaPlantilla.PLANOS_Y_PERMISOS, ((resultado.sugerencias.first().titulo) as TituloSugerido.Plantilla).tarea)
    }
}
