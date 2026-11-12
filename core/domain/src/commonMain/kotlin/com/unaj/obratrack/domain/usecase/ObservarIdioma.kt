package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.StateFlow

/** Idioma elegido; su valor actual es siempre el último guardado. */
class ObservarIdioma(private val repositorio: RepositorioPreferencias) {
    operator fun invoke(): StateFlow<IdiomaApp> = repositorio.idioma
}
