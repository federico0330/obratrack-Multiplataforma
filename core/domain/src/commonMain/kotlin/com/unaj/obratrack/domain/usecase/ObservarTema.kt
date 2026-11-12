package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.StateFlow

/** Tema elegido; su valor actual es siempre el último guardado. */
class ObservarTema(private val repositorio: RepositorioPreferencias) {
    operator fun invoke(): StateFlow<TemaApp> = repositorio.tema
}
