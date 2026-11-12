package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class RepositorioPreferenciasEnMemoria : RepositorioPreferencias {
    private val temaActual = MutableStateFlow(TemaApp.SISTEMA)
    private val idiomaActual = MutableStateFlow(IdiomaApp.SISTEMA)
    override val tema: StateFlow<TemaApp> = temaActual
    override val idioma: StateFlow<IdiomaApp> = idiomaActual

    override suspend fun guardarTema(tema: TemaApp) {
        temaActual.value = tema
    }

    override suspend fun guardarIdioma(idioma: IdiomaApp) {
        idiomaActual.value = idioma
    }
}

class PreferenciasTest {
    @Test
    fun cambiarTemaSeReflejaEnObservarTema() = runTest {
        val repository = RepositorioPreferenciasEnMemoria()
        val observar = ObservarTema(repository)

        assertEquals(TemaApp.SISTEMA, observar().value)
        CambiarTema(repository)(TemaApp.OSCURO)

        assertEquals(TemaApp.OSCURO, observar().value)
    }

    @Test
    fun cambiarIdiomaSeReflejaEnObservarIdioma() = runTest {
        val repository = RepositorioPreferenciasEnMemoria()
        val observar = ObservarIdioma(repository)

        assertEquals(IdiomaApp.SISTEMA, observar().value)
        CambiarIdioma(repository)(IdiomaApp.ITALIANO)

        assertEquals(IdiomaApp.ITALIANO, observar().value)
    }

    @Test
    fun soloElSistemaNoTieneCodigoDeIdioma() {
        assertEquals(null, IdiomaApp.SISTEMA.codigo)
        assertEquals(
            listOf("es", "en", "it"),
            IdiomaApp.entries.filter { it != IdiomaApp.SISTEMA }.map { it.codigo },
        )
    }
}
