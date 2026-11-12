package com.unaj.obratrack.composeapp

import com.unaj.obratrack.domain.model.IdiomaApp
import platform.Foundation.NSUserDefaults

private const val CLAVE_IDIOMAS_APPLE = "AppleLanguages"

/**
 * iOS elige el idioma de la app con `AppleLanguages`, que lee al arrancar el proceso: el cambio
 * puede no verse hasta abrir la app de nuevo. Con el idioma del sistema se quita el valor propio
 * de la app y vuelve a regir el del dispositivo.
 */
internal fun aplicarIdiomaIos(idioma: IdiomaApp) {
    val defaults = NSUserDefaults.standardUserDefaults
    val codigo = idioma.codigo
    if (codigo == null) {
        defaults.removeObjectForKey(CLAVE_IDIOMAS_APPLE)
    } else {
        defaults.setObject(listOf(codigo), forKey = CLAVE_IDIOMAS_APPLE)
    }
}
