package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

private const val DECIMALES_PANTALLA = 5
private const val DECIMALES_ENLACE = 6

/** `lat, lon` con 5 decimales (cerca de un metro). Siempre con punto: no depende del idioma del teléfono. */
internal fun formatearCoordenadas(ubicacion: Ubicacion): String =
    "${formatearGrados(ubicacion.latitud, DECIMALES_PANTALLA)}, ${formatearGrados(ubicacion.longitud, DECIMALES_PANTALLA)}"

/** Búsqueda de Google Maps por coordenadas; en Android abre la app de mapas y en iOS el navegador o la app instalada. */
internal fun enlaceMapa(ubicacion: Ubicacion): String =
    "https://www.google.com/maps/search/?api=1&query=" +
        "${formatearGrados(ubicacion.latitud, DECIMALES_ENLACE)},${formatearGrados(ubicacion.longitud, DECIMALES_ENLACE)}"

/** En common no hay `String.format`: se redondea con enteros para no caer en notación científica. */
internal fun formatearGrados(valor: Double, decimales: Int): String {
    if (!valor.isFinite()) return valor.toString()
    val escala = 10.0.pow(decimales).roundToLong()
    val redondeado = (abs(valor) * escala).roundToLong()
    val signo = if (valor < 0 && redondeado != 0L) "-" else ""
    return "$signo${redondeado / escala}.${(redondeado % escala).toString().padStart(decimales, '0')}"
}

/** Captura con el GPS: el ViewModel hace la búsqueda; acá solo se muestran el estado y las acciones. */
@Composable
internal fun SelectorUbicacion(form: FormularioRelevamientoUiState, acciones: RelevamientosAcciones) {
    Text(stringResource(Res.string.relevamientos_location_optional), style = MaterialTheme.typography.labelMedium)
    form.ubicacion?.let {
        Text(stringResource(Res.string.relevamientos_location, formatearCoordenadas(it)), modifier = Modifier.testTag("relevamientos_location_value"))
    }
    if (form.capturandoUbicacion) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(Res.string.relevamientos_location_searching), modifier = Modifier.weight(1f))
            TextButton(onClick = acciones.onLocationCancel, modifier = Modifier.testTag("relevamientos_location_cancel")) {
                Text(stringResource(Res.string.relevamientos_location_cancel))
            }
        }
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = acciones.onLocationCapture, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_location_capture")) {
                IconoBoton(Iconos.MyLocation)
                Text(stringResource(if (form.ubicacion == null) Res.string.relevamientos_location_use else Res.string.relevamientos_location_update))
            }
            if (form.ubicacion != null) {
                TextButton(onClick = acciones.onLocationRemove, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_location_remove")) {
                    IconoBoton(Iconos.Delete)
                    Text(stringResource(Res.string.relevamientos_location_remove))
                }
            }
        }
    }
}

/** Coordenadas del relevamiento y el botón para verlas en el mapa del teléfono. */
@Composable
internal fun UbicacionDetalle(ubicacion: Ubicacion) {
    val uriHandler = LocalUriHandler.current
    Text(stringResource(Res.string.relevamientos_location, formatearCoordenadas(ubicacion)), modifier = Modifier.testTag("relevamientos_location_value"))
    OutlinedButton(
        onClick = {
            try {
                uriHandler.openUri(enlaceMapa(ubicacion))
            } catch (_: Exception) {
                // Sin navegador ni app de mapas no hay otra forma de mostrarlo; las coordenadas siguen a la vista.
            }
        },
        modifier = Modifier.testTag("relevamientos_location_open_map"),
    ) {
        IconoBoton(Iconos.OpenInNew)
        Text(stringResource(Res.string.relevamientos_location_open_map))
    }
}
