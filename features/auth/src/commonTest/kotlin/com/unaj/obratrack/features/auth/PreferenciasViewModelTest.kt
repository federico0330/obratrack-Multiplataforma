package com.unaj.obratrack.features.auth

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import com.unaj.obratrack.domain.usecase.CambiarIdioma
import com.unaj.obratrack.domain.usecase.CambiarTema
import com.unaj.obratrack.domain.usecase.ObservarIdioma
import com.unaj.obratrack.domain.usecase.ObservarTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasViewModelTest {
    private fun conViewModel(
        tema: TemaApp = TemaApp.SISTEMA,
        idioma: IdiomaApp = IdiomaApp.SISTEMA,
        bloque: (PreferenciasViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repositorio = PreferenciasEnMemoria(tema, idioma)
        val viewModel = PreferenciasViewModel(
            ObservarTema(repositorio),
            CambiarTema(repositorio),
            ObservarIdioma(repositorio),
            CambiarIdioma(repositorio),
        )
        try {
            bloque(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun arrancaConLoGuardadoSinValorProvisorio() = conViewModel(TemaApp.OSCURO, IdiomaApp.ITALIANO) { viewModel ->
        assertEquals(TemaApp.OSCURO, viewModel.tema.value)
        assertEquals(IdiomaApp.ITALIANO, viewModel.idioma.value)
    }

    @Test
    fun elegirTemaActualizaElEstado() = conViewModel { viewModel ->
        viewModel.elegirTema(TemaApp.CLARO)

        assertEquals(TemaApp.CLARO, viewModel.tema.value)
    }

    @Test
    fun elegirIdiomaActualizaElEstadoSinTocarElTema() = conViewModel(tema = TemaApp.OSCURO) { viewModel ->
        viewModel.elegirIdioma(IdiomaApp.INGLES)

        assertEquals(IdiomaApp.INGLES, viewModel.idioma.value)
        assertEquals(TemaApp.OSCURO, viewModel.tema.value)
    }
}

private class PreferenciasEnMemoria(tema: TemaApp, idioma: IdiomaApp) : RepositorioPreferencias {
    private val temaActual = MutableStateFlow(tema)
    private val idiomaActual = MutableStateFlow(idioma)
    override val tema: StateFlow<TemaApp> = temaActual
    override val idioma: StateFlow<IdiomaApp> = idiomaActual

    override suspend fun guardarTema(tema: TemaApp) {
        temaActual.value = tema
    }

    override suspend fun guardarIdioma(idioma: IdiomaApp) {
        idiomaActual.value = idioma
    }
}
