package com.unaj.obratrack.domain.usecase

import com.unaj.obratrack.domain.model.CambioFoto
import com.unaj.obratrack.domain.model.CambioFotoUrl
import com.unaj.obratrack.domain.repository.AlmacenFotos
import com.unaj.obratrack.domain.repository.ErrorRelevamiento
import com.unaj.obratrack.domain.repository.ResultadoSubidaFoto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Nombre único por subida: con un nombre repetido, una subida sin firma no reemplaza la imagen anterior. */
@OptIn(ExperimentalUuidApi::class)
internal fun nombreFotoAleatorio(): String = Uuid.random().toString()

internal fun carpetaFotos(obraId: String) = "relevamientos/$obraId"

internal sealed interface FotoResuelta {
    data class Lista(val cambio: CambioFotoUrl) : FotoResuelta
    data class Error(val error: ErrorRelevamiento) : FotoResuelta
}

/** Sube la foto nueva, si la hay. Se llama antes de cualquier escritura en el repositorio. */
internal suspend fun resolverFoto(
    cambio: CambioFoto, obraId: String, almacen: AlmacenFotos, generarNombre: () -> String,
): FotoResuelta = when (cambio) {
    CambioFoto.SinCambio -> FotoResuelta.Lista(CambioFotoUrl.SinCambio)
    CambioFoto.Quitar -> FotoResuelta.Lista(CambioFotoUrl.Quitar)
    is CambioFoto.Nueva -> when (val subida = almacen.subir(cambio.bytes, carpetaFotos(obraId), generarNombre())) {
        is ResultadoSubidaFoto.Exito -> FotoResuelta.Lista(CambioFotoUrl.Nueva(subida.url))
        ResultadoSubidaFoto.SinConexion -> FotoResuelta.Error(ErrorRelevamiento.FOTO_SIN_CONEXION)
        ResultadoSubidaFoto.MuyGrande -> FotoResuelta.Error(ErrorRelevamiento.FOTO_MUY_GRANDE)
        ResultadoSubidaFoto.Fallo -> FotoResuelta.Error(ErrorRelevamiento.FOTO_FALLO)
    }
}
