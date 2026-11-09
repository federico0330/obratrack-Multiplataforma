package com.unaj.obratrack.core.data.fotos

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.compressImage

/** Lado más largo de la foto que se sube; alcanza para ver el detalle de una obra y pesa poco. */
internal const val LADO_MAXIMO_PX = 1920

/** Peso al que se intenta llegar bajando la calidad de a poco. */
internal const val PESO_OBJETIVO_BYTES = 1_500_000

/** Tope de lo que se sube aunque no se haya llegado al objetivo. */
internal const val PESO_MAXIMO_BYTES = 5 * 1024 * 1024

internal val CALIDADES_JPEG = listOf(85, 75, 65, 50, 40)

/** Codifica en JPEG, con la calidad pedida, una imagen ya decodificada y achicada. */
typealias CodificarJpeg = suspend (calidad: Int) -> ByteArray

/**
 * Achica una imagen y deja que [elegir] la codifique con las calidades que necesite. Así una plataforma
 * puede decodificar la foto una sola vez. Lanza una excepción si los bytes no son una imagen.
 */
fun interface CompresorImagen {
    suspend fun comprimir(bytes: ByteArray, ladoMaximo: Int, elegir: suspend (CodificarJpeg) -> ByteArray?): ByteArray?
}

/** Compresión de FileKit: conserva la proporción, pero decodifica la imagen completa en cada calidad. */
internal val compresorFileKit = CompresorImagen { bytes, ladoMaximo, elegir ->
    elegir { calidad -> FileKit.compressImage(bytes, ImageFormat.JPEG, calidad, maxWidth = ladoMaximo, maxHeight = ladoMaximo) }
}

/** Compresor de cada plataforma: en Android uno propio que reduce la foto al leerla; en iOS, FileKit. */
internal expect val compresorPlataforma: CompresorImagen

/**
 * Prueba las calidades en orden y devuelve la primera salida que llega al objetivo. Si ninguna llega,
 * usa la de menor calidad mientras no pase el tope; si lo pasa, devuelve null.
 */
internal suspend fun prepararFoto(bytes: ByteArray, compresor: CompresorImagen): ByteArray? =
    compresor.comprimir(bytes, LADO_MAXIMO_PX) { codificar ->
        var ultima: ByteArray? = null
        for (calidad in CALIDADES_JPEG) {
            val salida = codificar(calidad)
            if (salida.size <= PESO_OBJETIVO_BYTES) return@comprimir salida
            ultima = salida
        }
        ultima?.takeIf { it.size <= PESO_MAXIMO_BYTES }
    }

/**
 * Mayor potencia de 2 por la que se puede dividir la imagen al leerla sin que el lado más largo quede
 * por debajo de [ladoMaximo]. Una foto de 50 MP se lee a un cuarto y no ocupa 200 MB en memoria.
 */
internal fun factorMuestreo(ancho: Int, alto: Int, ladoMaximo: Int): Int {
    val lado = maxOf(ancho, alto)
    var factor = 1
    while (lado / (factor * 2) >= ladoMaximo) factor *= 2
    return factor
}

/** Medidas finales con el lado más largo en [ladoMaximo] como mucho, conservando la proporción. */
internal fun medidasReducidas(ancho: Int, alto: Int, ladoMaximo: Int): Pair<Int, Int> {
    val lado = maxOf(ancho, alto)
    if (lado <= ladoMaximo) return ancho to alto
    val escala = ladoMaximo.toDouble() / lado
    return maxOf(1, (ancho * escala).toInt()) to maxOf(1, (alto * escala).toInt())
}
