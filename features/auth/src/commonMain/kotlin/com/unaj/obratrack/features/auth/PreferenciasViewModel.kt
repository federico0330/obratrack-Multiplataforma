package com.unaj.obratrack.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.usecase.CambiarTema
import com.unaj.obratrack.domain.usecase.ObservarTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Preferencias del dispositivo. El tema se aplica en la raíz de la app, no solo en Perfil. */
class PreferenciasViewModel(
    observarTema: ObservarTema,
    private val cambiarTema: CambiarTema,
) : ViewModel() {
    val tema: StateFlow<TemaApp> = observarTema()
        .stateIn(viewModelScope, SharingStarted.Eagerly, TemaApp.SISTEMA)

    fun elegirTema(tema: TemaApp) {
        viewModelScope.launch { cambiarTema(tema) }
    }
}
