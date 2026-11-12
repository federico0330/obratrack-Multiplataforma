package com.unaj.obratrack.features.auth

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import com.unaj.obratrack.domain.usecase.CambiarTema
import com.unaj.obratrack.domain.usecase.ObservarTema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenciasViewModelTest {
    private fun conViewModel(inicial: TemaApp, bloque: (PreferenciasViewModel) -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repositorio = PreferenciasEnMemoria(inicial)
        val viewModel = PreferenciasViewModel(ObservarTema(repositorio), CambiarTema(repositorio))
        try {
            bloque(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun arrancaConElTemaGuardado() = conViewModel(TemaApp.OSCURO) { viewModel ->
        assertEquals(TemaApp.OSCURO, viewModel.tema.value)
    }

    @Test
    fun elegirTemaActualizaElEstado() = conViewModel(TemaApp.SISTEMA) { viewModel ->
        viewModel.elegirTema(TemaApp.CLARO)

        assertEquals(TemaApp.CLARO, viewModel.tema.value)
    }
}

private class PreferenciasEnMemoria(inicial: TemaApp) : RepositorioPreferencias {
    private val actual = MutableStateFlow(inicial)
    override val tema: Flow<TemaApp> = actual

    override suspend fun guardarTema(tema: TemaApp) {
        actual.value = tema
    }
}
