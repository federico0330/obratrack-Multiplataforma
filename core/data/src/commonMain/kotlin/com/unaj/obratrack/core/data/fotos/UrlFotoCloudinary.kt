package com.unaj.obratrack.core.data.fotos

/** Ancho con el que se pide la foto para mostrarla en pantalla. */
internal const val ANCHO_VISTA_PX = 800

/** Lado de la miniatura cuadrada del listado: 64 dp en pantallas de hasta 2,5x. */
internal const val LADO_MINIATURA_PX = 160

private const val SEGMENTO_SUBIDA = "/image/upload/"
private val TRANSFORMACION = Regex("^[a-z]{1,3}_[^/]*/")

/** Pide a Cloudinary una versión reducida a [ancho], conservando la proporción. */
fun urlReducidaCloudinary(url: String, ancho: Int = ANCHO_VISTA_PX): String =
    conTransformacion(url, "c_fill,w_$ancho")

/** Pide a Cloudinary un recorte cuadrado de [lado] px para la miniatura del listado. */
fun urlMiniaturaCloudinary(url: String, lado: Int = LADO_MINIATURA_PX): String =
    conTransformacion(url, "c_fill,w_$lado,h_$lado")

/**
 * Inserta la transformación después de `/image/upload/`. Una URL que no es de Cloudinary, o que ya trae
 * una transformación, se devuelve igual.
 */
private fun conTransformacion(url: String, transformacion: String): String {
    if (!url.startsWith("https://res.cloudinary.com/") && !url.startsWith("http://res.cloudinary.com/")) return url
    val inicio = url.indexOf(SEGMENTO_SUBIDA)
    if (inicio < 0) return url
    val corte = inicio + SEGMENTO_SUBIDA.length
    val resto = url.substring(corte)
    if (TRANSFORMACION.containsMatchIn(resto)) return url
    return url.substring(0, corte) + "$transformacion/" + resto
}
