package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.repository.RepositorioObras
import com.unaj.obratrack.domain.validation.ResultadoValidacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CrearObraTest {
    @Test
    fun nombreValidoSeNormalizaYSeGuarda() {
        val repositorio = RepositorioObrasEnMemoria()
        val casoDeUso = CrearObra(repositorio)

        val resultado = casoDeUso(id = "obra-1", nombre = "  Escuela  ")

        assertEquals(ResultadoCrearObra.Creada(Obra("obra-1", "Escuela")), resultado)
        assertEquals(listOf(Obra("obra-1", "Escuela")), repositorio.obras)
    }

    @Test
    fun nombreVacioSeRechazaSinGuardar() {
        val repositorio = RepositorioObrasEnMemoria()
        val casoDeUso = CrearObra(repositorio)

        val resultado = casoDeUso(id = "obra-1", nombre = "   ")

        assertEquals(
            ResultadoCrearObra.Rechazada(ResultadoValidacion.NombreVacio),
            resultado,
        )
        assertEquals(emptyList(), repositorio.obras)
    }

    @Test
    fun nombreMayorA80CaracteresSeRechaza() {
        val repositorio = RepositorioObrasEnMemoria()
        val resultado = CrearObra(repositorio)(id = "obra-1", nombre = "a".repeat(81))

        val rechazo = assertIs<ResultadoCrearObra.Rechazada>(resultado)
        assertEquals(ResultadoValidacion.NombreDemasiadoLargo, rechazo.motivo)
        assertEquals(emptyList(), repositorio.obras)
    }
}

private class RepositorioObrasEnMemoria : RepositorioObras {
    val obras = mutableListOf<Obra>()

    override suspend fun crear(obra: Obra) {
        obras += obra
    }
}
