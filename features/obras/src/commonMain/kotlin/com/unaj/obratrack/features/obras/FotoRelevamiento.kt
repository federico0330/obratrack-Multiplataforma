package com.unaj.obratrack.features.obras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Relevamiento
import com.unaj.obratrack.features.obras.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** La foto guardada, solo si es una URL web: otros valores se conservan pero no se muestran. */
internal fun Relevamiento.fotoWeb(): String? = fotoUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

/** Muestra una foto (URL o bytes recién elegidos) con estados de carga y error. */
@Composable
internal fun FotoRelevamiento(model: Any, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = stringResource(Res.string.relevamientos_photo_description),
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium).testTag("relevamientos_photo"),
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        },
        error = {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.relevamientos_photo_error), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

/** Miniatura del listado. Es decorativa: la tarjeta ya tiene el texto y abre el detalle. */
@Composable
internal fun MiniaturaRelevamiento(url: String) {
    val fondo = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small).testTag("relevamientos_thumbnail"),
        loading = { Box(fondo) },
        error = {
            Box(fondo, contentAlignment = Alignment.Center) {
                Icon(painterResource(Iconos.Image), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/**
 * Selector de la galería y de la cámara. Los launchers viven acá porque dependen de la plataforma;
 * el ViewModel solo recibe los bytes ya leídos.
 */
@Composable
internal fun SelectorFoto(form: FormularioRelevamientoUiState, acciones: RelevamientosAcciones) {
    val scope = rememberCoroutineScope()
    val galeria = rememberFilePickerLauncher(type = FileKitType.Image, onError = { acciones.onPhotoReadError() }) { archivo ->
        if (archivo != null) scope.launch { leerFoto(archivo, temporal = false, acciones) }
    }
    // Cancelar, o negar el permiso si alguna vez se declara, devuelve null: no es un error.
    val camara = rememberCameraPickerLauncher(onError = { acciones.onCameraError() }) { archivo ->
        if (archivo != null) scope.launch { leerFoto(archivo, temporal = true, acciones) }
    }
    Text(stringResource(Res.string.relevamientos_photo), style = MaterialTheme.typography.labelMedium)
    when (val foto = form.foto) {
        FotoFormulario.Ninguna -> Unit
        is FotoFormulario.Existente -> FotoRelevamiento(foto.vistaUrl)
        is FotoFormulario.Nueva -> FotoRelevamiento(foto.bytes)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { galeria.launch() }, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_photo_pick")) {
            IconoBoton(Iconos.AddPhoto)
            Text(stringResource(Res.string.relevamientos_photo_pick))
        }
        OutlinedButton(onClick = { camara.launch() }, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_photo_camera")) {
            IconoBoton(Iconos.Camera)
            Text(stringResource(Res.string.relevamientos_photo_take))
        }
        if (form.foto != FotoFormulario.Ninguna) {
            TextButton(onClick = acciones.onRemovePhoto, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_photo_remove")) {
                IconoBoton(Iconos.Delete)
                Text(stringResource(Res.string.relevamientos_photo_remove))
            }
        }
    }
}

private suspend fun leerFoto(archivo: PlatformFile, temporal: Boolean, acciones: RelevamientosAcciones) {
    try {
        acciones.onPhotoPicked(archivo.readBytes())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        acciones.onPhotoReadError()
    } finally {
        // La cámara deja la foto en la caché de la app; una vez leída no hace falta conservarla.
        if (temporal) withContext(NonCancellable) { runCatching { archivo.delete(mustExist = false) } }
    }
}
