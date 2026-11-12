package com.unaj.obratrack.core.data.preferencias

import com.russhwolf.settings.Settings
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Guarda las preferencias en el almacenamiento clave-valor de cada plataforma. La lectura es
 * síncrona y se hace al crear el repositorio, así el primer cuadro ya sale con el tema elegido.
 */
class RepositorioPreferenciasImpl(
    private val settings: Settings,
) : RepositorioPreferencias {
    private val temaActual = MutableStateFlow(leerTema())

    override val tema: Flow<TemaApp> = temaActual

    override suspend fun guardarTema(tema: TemaApp) {
        settings.putString(CLAVE_TEMA, tema.name)
        temaActual.value = tema
    }

    // Un valor ausente o desconocido (p. ej. de una versión futura) vuelve al tema del sistema.
    private fun leerTema(): TemaApp =
        TemaApp.entries.firstOrNull { it.name == settings.getStringOrNull(CLAVE_TEMA) } ?: TemaApp.SISTEMA

    private companion object {
        const val CLAVE_TEMA = "tema"
    }
}

/** Repositorio sobre el almacenamiento por defecto de la plataforma (SharedPreferences / NSUserDefaults). */
fun crearRepositorioPreferencias(): RepositorioPreferencias = RepositorioPreferenciasImpl(Settings())
