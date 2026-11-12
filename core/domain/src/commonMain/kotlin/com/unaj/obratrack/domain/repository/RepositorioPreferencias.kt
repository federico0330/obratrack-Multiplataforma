package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.TemaApp
import kotlinx.coroutines.flow.Flow

/** Preferencias del dispositivo, no de la cuenta: se guardan localmente y no pasan por Firebase. */
interface RepositorioPreferencias {
    val tema: Flow<TemaApp>
    suspend fun guardarTema(tema: TemaApp)
}
