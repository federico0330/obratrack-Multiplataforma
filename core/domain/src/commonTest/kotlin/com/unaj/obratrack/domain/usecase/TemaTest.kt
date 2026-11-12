package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class RepositorioPreferenciasEnMemoria : RepositorioPreferencias {
    private val actual = MutableStateFlow(TemaApp.SISTEMA)
    override val tema: Flow<TemaApp> = actual

    override suspend fun guardarTema(tema: TemaApp) {
        actual.value = tema
    }
}

class TemaTest {
    @Test
    fun cambiarTemaSeReflejaEnObservarTema() = runTest {
        val repository = RepositorioPreferenciasEnMemoria()
        val observar = ObservarTema(repository)

        assertEquals(TemaApp.SISTEMA, observar().first())
        CambiarTema(repository)(TemaApp.OSCURO)

        assertEquals(TemaApp.OSCURO, observar().first())
    }
}
