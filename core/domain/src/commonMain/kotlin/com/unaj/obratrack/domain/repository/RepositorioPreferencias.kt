package com.unaj.obratrack.domain.repository

import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import kotlinx.coroutines.flow.StateFlow

/**
 * Preferencias del dispositivo, no de la cuenta: se guardan localmente y no pasan por Firebase.
 * Se exponen como [StateFlow] porque la lectura es local y síncrona: el valor guardado está
 * disponible desde el primer cuadro, sin un valor provisorio.
 */
interface RepositorioPreferencias {
    val tema: StateFlow<TemaApp>
    val idioma: StateFlow<IdiomaApp>
    suspend fun guardarTema(tema: TemaApp)
    suspend fun guardarIdioma(idioma: IdiomaApp)
}
