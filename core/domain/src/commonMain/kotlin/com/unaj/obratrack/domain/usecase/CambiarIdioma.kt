package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias

class CambiarIdioma(private val repositorio: RepositorioPreferencias) {
    suspend operator fun invoke(idioma: IdiomaApp) = repositorio.guardarIdioma(idioma)
}
