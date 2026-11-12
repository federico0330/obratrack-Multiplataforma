package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias

class CambiarTema(private val repositorio: RepositorioPreferencias) {
    suspend operator fun invoke(tema: TemaApp) = repositorio.guardarTema(tema)
}
