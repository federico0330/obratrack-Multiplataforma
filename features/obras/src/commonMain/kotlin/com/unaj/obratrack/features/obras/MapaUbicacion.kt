package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private fun camaraEn(ubicacion: Ubicacion, zoom: Double) =
    CameraPosition(center = Position(longitude = ubicacion.longitud, latitude = ubicacion.latitud), zoom = zoom)

/** El pin queda siempre en el centro del mapa, con la punta justo sobre el punto. */
@Composable
private fun PinDelMapa(modifier: Modifier = Modifier) {
    // La punta del ícono está al 92 % de su alto: se sube lo que falta para que apunte al centro.
    Icon(
        painter = painterResource(Iconos.Place), contentDescription = null, tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(40.dp).offset(y = (-17).dp),
    )
}

/**
 * Mapa chico con el pin en la ubicación. No se mueve con los dedos, así la pantalla sigue desplazándose.
 * Si la ubicación cambia (GPS, dirección o mapa) la cámara se mueve sin volver a crear el mapa.
 */
@Composable
internal fun MapaVistaPrevia(ubicacion: Ubicacion, modifier: Modifier = Modifier) {
    val estado = rememberMapState(baseStyle = BaseStyle.Uri(ESTILO_MAPA), initialCameraPosition = camaraEn(ubicacion, ZOOM_VISTA_PREVIA))
    LaunchedEffect(ubicacion) { estado.setCameraPosition(camaraEn(ubicacion, ZOOM_VISTA_PREVIA)) }
    val descripcion = stringResource(Res.string.relevamientos_map_description)
    Box(modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium).semantics { contentDescription = descripcion }.testTag("relevamientos_map_preview")) {
        // Solo el crédito del mapa: es un requisito de los datos, así que no se saca.
        MaplibreMap(state = estado, uiOptions = MapUiOptions.None, overlay = { include(MapOverlay.AttributionOnly) })
        PinDelMapa(Modifier.align(Alignment.Center))
    }
}

/**
 * Pantalla completa para elegir el lugar: se mueve el mapa por debajo de un pin fijo y se confirma. Es un diálogo
 * porque el mapa tiene que recibir los gestos sin pelearse con el desplazamiento de la pantalla del relevamiento.
 */
@Composable
internal fun SelectorEnMapa(centro: CentroMapa, onConfirmar: (Ubicacion) -> Unit, onCancelar: () -> Unit) {
    Dialog(onDismissRequest = onCancelar, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().testTag("relevamientos_map_picker")) {
            val estado = rememberMapState(baseStyle = BaseStyle.Uri(ESTILO_MAPA), initialCameraPosition = camaraEn(centro.ubicacion, centro.zoom))
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text(stringResource(Res.string.relevamientos_map_pick_hint), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    MaplibreMap(state = estado, overlay = { include(MapOverlay.Full) })
                    PinDelMapa(Modifier.align(Alignment.Center))
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f).testTag("relevamientos_map_cancel")) {
                        Text(stringResource(Res.string.relevamientos_map_pick_cancel))
                    }
                    Button(
                        onClick = {
                            val centroActual = estado.cameraPosition.center
                            ubicacionDelCentro(centroActual.latitude, centroActual.longitude)?.let(onConfirmar)
                        },
                        modifier = Modifier.weight(1f).testTag("relevamientos_map_confirm"),
                    ) {
                        IconoBoton(Iconos.Place)
                        Text(stringResource(Res.string.relevamientos_map_pick_confirm))
                    }
                }
            }
        }
    }
}
