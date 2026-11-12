package com.unaj.obratrack.core.data.preferencias

import com.russhwolf.settings.MapSettings
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositorioPreferenciasImplTest {
    @Test
    fun sinNadaGuardadoSigueAlSistema() = runTest {
        assertEquals(TemaApp.SISTEMA, RepositorioPreferenciasImpl(MapSettings()).tema.first())
    }

    @Test
    fun guardarEmiteYSobreviveAUnaNuevaInstancia() = runTest {
        val settings = MapSettings()
        val repositorio = RepositorioPreferenciasImpl(settings)

        repositorio.guardarTema(TemaApp.OSCURO)

        assertEquals(TemaApp.OSCURO, repositorio.tema.first())
        assertEquals(TemaApp.OSCURO, RepositorioPreferenciasImpl(settings).tema.first())
    }

    @Test
    fun valorDesconocidoVuelveAlSistema() = runTest {
        val settings = MapSettings("tema" to "SEPIA", "idioma" to "KLINGON")
        val repositorio = RepositorioPreferenciasImpl(settings)

        assertEquals(TemaApp.SISTEMA, repositorio.tema.first())
        assertEquals(IdiomaApp.SISTEMA, repositorio.idioma.first())
    }

    @Test
    fun idiomaSinNadaGuardadoSigueAlSistema() = runTest {
        assertEquals(IdiomaApp.SISTEMA, RepositorioPreferenciasImpl(MapSettings()).idioma.value)
    }

    @Test
    fun guardarIdiomaPersisteAntesDeEmitir() = runTest {
        val settings = MapSettings()
        val repositorio = RepositorioPreferenciasImpl(settings)

        repositorio.guardarIdioma(IdiomaApp.ITALIANO)

        assertEquals(IdiomaApp.ITALIANO, repositorio.idioma.value)
        assertEquals("ITALIANO", settings.getStringOrNull("idioma"))
        assertEquals(IdiomaApp.ITALIANO, RepositorioPreferenciasImpl(settings).idioma.value)
    }

    @Test
    fun elIdiomaYElTemaSonIndependientes() = runTest {
        val repositorio = RepositorioPreferenciasImpl(MapSettings())

        repositorio.guardarIdioma(IdiomaApp.INGLES)

        assertEquals(TemaApp.SISTEMA, repositorio.tema.value)
    }
}
