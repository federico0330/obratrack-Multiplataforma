package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.Flow

/** Emite el tema elegido; al suscribirse emite de inmediato el último guardado. */
class ObservarTema(private val repositorio: RepositorioPreferencias) {
    operator fun invoke(): Flow<TemaApp> = repositorio.tema
}
