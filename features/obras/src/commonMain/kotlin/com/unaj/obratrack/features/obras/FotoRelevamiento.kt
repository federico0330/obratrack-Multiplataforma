package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.unaj.obratrack.features.obras.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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

/**
 * Selector de la galería. El launcher vive acá porque depende de la plataforma; el ViewModel
 * solo recibe los bytes ya leídos.
 */
@Composable
internal fun SelectorFoto(form: FormularioRelevamientoUiState, acciones: RelevamientosAcciones) {
    val scope = rememberCoroutineScope()
    val galeria = rememberFilePickerLauncher(type = FileKitType.Image, onError = { acciones.onPhotoReadError() }) { archivo ->
        if (archivo != null) scope.launch {
            try {
                acciones.onPhotoPicked(archivo.readBytes())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                acciones.onPhotoReadError()
            }
        }
    }
    Text(stringResource(Res.string.relevamientos_photo), style = MaterialTheme.typography.labelMedium)
    when (val foto = form.foto) {
        FotoFormulario.Ninguna -> Unit
        is FotoFormulario.Existente -> FotoRelevamiento(foto.vistaUrl)
        is FotoFormulario.Nueva -> FotoRelevamiento(foto.bytes)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { galeria.launch() }, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_photo_pick")) {
            IconoBoton(Iconos.AddPhoto)
            Text(stringResource(if (form.foto == FotoFormulario.Ninguna) Res.string.relevamientos_photo_pick else Res.string.relevamientos_photo_change))
        }
        if (form.foto != FotoFormulario.Ninguna) {
            TextButton(onClick = acciones.onRemovePhoto, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_photo_remove")) {
                IconoBoton(Iconos.Delete)
                Text(stringResource(Res.string.relevamientos_photo_remove))
            }
        }
    }
}
