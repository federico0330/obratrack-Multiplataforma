package com.unaj.obratrack.core.data.preferencias

import com.russhwolf.settings.Settings
import com.unaj.obratrack.domain.model.IdiomaApp
import com.unaj.obratrack.domain.model.TemaApp
import com.unaj.obratrack.domain.repository.RepositorioPreferencias
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda las preferencias en el almacenamiento clave-valor de cada plataforma. La lectura es
 * síncrona y se hace al crear el repositorio, así el primer cuadro ya sale con lo elegido.
 */
class RepositorioPreferenciasImpl(
    private val settings: Settings,
) : RepositorioPreferencias {
    private val temaActual = MutableStateFlow(leer(CLAVE_TEMA, TemaApp.entries, TemaApp.SISTEMA))
    private val idiomaActual = MutableStateFlow(leer(CLAVE_IDIOMA, IdiomaApp.entries, IdiomaApp.SISTEMA))

    override val tema: StateFlow<TemaApp> = temaActual.asStateFlow()
    override val idioma: StateFlow<IdiomaApp> = idiomaActual.asStateFlow()

    override suspend fun guardarTema(tema: TemaApp) {
        settings.putString(CLAVE_TEMA, tema.name)
        temaActual.value = tema
    }

    // Se guarda antes de emitir: quien reacciona al cambio (p. ej. reiniciar la pantalla en Android)
    // ya lo encuentra en el almacenamiento.
    override suspend fun guardarIdioma(idioma: IdiomaApp) {
        settings.putString(CLAVE_IDIOMA, idioma.name)
        idiomaActual.value = idioma
    }

    // Un valor ausente o desconocido (p. ej. de una versión futura) vuelve a la opción por defecto.
    private fun <T : Enum<T>> leer(clave: String, opciones: List<T>, porDefecto: T): T =
        opciones.firstOrNull { it.name == settings.getStringOrNull(clave) } ?: porDefecto

    private companion object {
        const val CLAVE_TEMA = "tema"
        const val CLAVE_IDIOMA = "idioma"
    }
}

/** Repositorio sobre el almacenamiento por defecto de la plataforma (SharedPreferences / NSUserDefaults). */
fun crearRepositorioPreferencias(): RepositorioPreferencias = RepositorioPreferenciasImpl(Settings())

/**
 * Idioma guardado, para plataformas que lo necesitan antes de que exista el grafo de la app
 * (Android lo aplica al crear la actividad).
 */
fun leerIdiomaGuardado(): IdiomaApp = crearRepositorioPreferencias().idioma.value
