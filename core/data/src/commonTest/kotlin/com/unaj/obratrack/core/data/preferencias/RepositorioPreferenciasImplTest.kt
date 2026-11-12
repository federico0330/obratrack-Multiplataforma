package com.unaj.obratrack.core.data.preferencias

import com.russhwolf.settings.MapSettings
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
        val settings = MapSettings("tema" to "SEPIA")

        assertEquals(TemaApp.SISTEMA, RepositorioPreferenciasImpl(settings).tema.first())
    }
}
