package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion

class CerrarSesion(private val repositorio: RepositorioAutenticacion) {
    suspend operator fun invoke(): ResultadoCerrarSesion = repositorio.cerrarSesion()
}
