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

/** Achica y recomprime una imagen en JPEG; lanza una excepción si no la puede decodificar. */
fun interface CompresorImagen {
    suspend fun comprimir(bytes: ByteArray, calidad: Int, ladoMaximo: Int): ByteArray
}

/** Usa la compresión de FileKit: corrige la orientación EXIF en Android y conserva la proporción. */
internal val compresorFileKit = CompresorImagen { bytes, calidad, ladoMaximo ->
    FileKit.compressImage(bytes, ImageFormat.JPEG, calidad, maxWidth = ladoMaximo, maxHeight = ladoMaximo)
}

/**
 * Prueba las calidades en orden y devuelve la primera salida que llega al objetivo. Si ninguna llega,
 * usa la de menor calidad mientras no pase el tope; si lo pasa, devuelve null.
 */
internal suspend fun prepararFoto(bytes: ByteArray, compresor: CompresorImagen): ByteArray? {
    var ultima: ByteArray? = null
    for (calidad in CALIDADES_JPEG) {
        val salida = compresor.comprimir(bytes, calidad, LADO_MAXIMO_PX)
        if (salida.size <= PESO_OBJETIVO_BYTES) return salida
        ultima = salida
    }
    return ultima?.takeIf { it.size <= PESO_MAXIMO_BYTES }
}
