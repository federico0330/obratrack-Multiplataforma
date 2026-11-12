package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.usecase.CambiarIdioma
import com.unaj.obratrack.domain.usecase.CambiarTema
import com.unaj.obratrack.domain.usecase.ObservarIdioma
import com.unaj.obratrack.domain.usecase.ObservarTema
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Preferencias del dispositivo. Tema e idioma se aplican en la raíz de la app, no solo en Perfil. */
class PreferenciasViewModel(
    observarTema: ObservarTema,
    private val cambiarTema: CambiarTema,
    observarIdioma: ObservarIdioma,
    private val cambiarIdioma: CambiarIdioma,
) : ViewModel() {
    val tema: StateFlow<TemaApp> = observarTema()
    val idioma: StateFlow<IdiomaApp> = observarIdioma()

    fun elegirTema(tema: TemaApp) {
        viewModelScope.launch { cambiarTema(tema) }
    }

    fun elegirIdioma(idioma: IdiomaApp) {
        viewModelScope.launch { cambiarIdioma(idioma) }
    }
}
