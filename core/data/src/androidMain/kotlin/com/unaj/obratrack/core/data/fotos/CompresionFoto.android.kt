package com.unaj.obratrack.core.data.fotos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Lee la foto ya reducida (inSampleSize), la lleva al lado máximo, la endereza según su EXIF y la codifica
 * con cada calidad sin volver a decodificarla.
 */
internal actual val compresorPlataforma: CompresorImagen = CompresorImagen { bytes, ladoMaximo, elegir ->
    val imagen = leerReducida(bytes, ladoMaximo)
    try {
        elegir { calidad ->
            ByteArrayOutputStream().also { imagen.compress(Bitmap.CompressFormat.JPEG, calidad, it) }.toByteArray()
        }
    } finally {
        imagen.recycle()
    }
}

private fun leerReducida(bytes: ByteArray, ladoMaximo: Int): Bitmap {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
    require(limites.outWidth > 0 && limites.outHeight > 0) { "No es una imagen" }
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = factorMuestreo(limites.outWidth, limites.outHeight, ladoMaximo)
    }
    val leida = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)) { "No es una imagen" }
    val (ancho, alto) = medidasReducidas(leida.width, leida.height, ladoMaximo)
    val reducida = reemplazar(leida, Bitmap.createScaledBitmap(leida, ancho, alto, true))
    return enderezar(reducida, bytes)
}

/** Aplica la orientación EXIF (rotación y espejado) después de achicar, para rotar la imagen chica. */
private fun enderezar(imagen: Bitmap, bytes: ByteArray): Bitmap {
    val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull() ?: return imagen
    val grados = exif.rotationDegrees
    if (grados == 0 && !exif.isFlipped) return imagen
    val matriz = Matrix().apply {
        setRotate(grados.toFloat())
        // EXIF define el giro después del espejo horizontal.
        if (exif.isFlipped) preScale(-1f, 1f)
    }
    return reemplazar(imagen, Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, matriz, true))
}

/** Libera la imagen anterior en cuanto existe la nueva, para no tener las dos en memoria. */
private fun reemplazar(anterior: Bitmap, nueva: Bitmap): Bitmap {
    if (nueva !== anterior) anterior.recycle()
    return nueva
}
