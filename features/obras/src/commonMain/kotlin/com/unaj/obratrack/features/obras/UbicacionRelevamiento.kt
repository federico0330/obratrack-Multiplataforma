package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.Ubicacion
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

private const val DECIMALES_ENLACE = 6

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

/**
 * Contenido del bloque Ubicación del formulario: el mapa muestra dónde queda, y se puede capturar con el GPS, elegir
 * en el mapa o buscar una dirección. El título del bloque lo pone [SeccionFormulario]. El ViewModel hace el trabajo;
 * acá solo se muestran el estado y las acciones.
 */
@Composable
internal fun SelectorUbicacion(form: FormularioRelevamientoUiState, acciones: RelevamientosAcciones) {
    form.ubicacion?.let { MapaVistaPrevia(it) }
    if (form.capturandoUbicacion) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(Res.string.relevamientos_location_searching), modifier = Modifier.weight(1f))
            TextButton(onClick = acciones.onLocationCancel, modifier = Modifier.testTag("relevamientos_location_cancel")) {
                Text(stringResource(Res.string.relevamientos_location_cancel))
            }
        }
    } else {
        // Las dos formas de ubicarse, lado a lado y del mismo tamaño aunque un texto ocupe dos líneas.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = acciones.onLocationCapture, enabled = !form.guardando, modifier = Modifier.weight(1f).fillMaxHeight().testTag("relevamientos_location_capture")) {
                IconoBoton(Iconos.MyLocation)
                Text(stringResource(if (form.ubicacion == null) Res.string.relevamientos_location_use else Res.string.relevamientos_location_update))
            }
            OutlinedButton(onClick = acciones.onMapOpen, enabled = !form.guardando, modifier = Modifier.weight(1f).fillMaxHeight().testTag("relevamientos_map_open")) {
                IconoBoton(Iconos.Map)
                Text(stringResource(Res.string.relevamientos_location_pick_on_map))
            }
        }
        // Sin geocodificador en el dispositivo el buscador no se ofrece.
        if (form.busqueda.disponible) BuscadorDireccion(form.busqueda, form.guardando, acciones)
        if (form.ubicacion != null) {
            TextButton(onClick = acciones.onLocationRemove, enabled = !form.guardando, modifier = Modifier.testTag("relevamientos_location_remove")) {
                IconoBoton(Iconos.Delete)
                Text(stringResource(Res.string.relevamientos_location_remove))
            }
        }
    }
}

/** Elegir la ubicación escribiendo una dirección: hasta 5 candidatos y el elegido se carga como ubicación nueva. */
@Composable
private fun BuscadorDireccion(busqueda: BusquedaDireccionUiState, deshabilitado: Boolean, acciones: RelevamientosAcciones) {
    if (!busqueda.abierta) {
        OutlinedButton(onClick = acciones.onAddressSearchOpen, enabled = !deshabilitado, modifier = Modifier.fillMaxWidth().testTag("relevamientos_address_open")) {
            IconoBoton(Iconos.Search)
            Text(stringResource(Res.string.relevamientos_address_search))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = busqueda.texto, onValueChange = acciones.onAddressTextChange, singleLine = true,
            label = { Text(stringResource(Res.string.relevamientos_address_label)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { acciones.onAddressSearch() }),
            enabled = !deshabilitado,
            modifier = Modifier.fillMaxWidth().testTag("relevamientos_address_text"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = acciones.onAddressSearch, enabled = busqueda.puedeBuscar && !deshabilitado, modifier = Modifier.testTag("relevamientos_address_search")) {
                if (busqueda.resultados == ResultadosBusquedaUi.Buscando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.relevamientos_address_search_action))
            }
            TextButton(onClick = acciones.onAddressSearchClose, modifier = Modifier.testTag("relevamientos_address_close")) {
                Text(stringResource(Res.string.relevamientos_address_cancel))
            }
        }
        when (val resultados = busqueda.resultados) {
            ResultadosBusquedaUi.SinBuscar, ResultadosBusquedaUi.Buscando -> Unit
            is ResultadosBusquedaUi.Lista -> resultados.items.forEach { candidato ->
                OutlinedCard(onClick = { acciones.onAddressPick(candidato) }, enabled = !deshabilitado, modifier = Modifier.fillMaxWidth().testTag("relevamientos_address_result")) {
                    Text(candidato.texto, modifier = Modifier.padding(12.dp))
                }
            }
            ResultadosBusquedaUi.SinResultados -> Text(stringResource(Res.string.relevamientos_address_no_results), style = MaterialTheme.typography.bodySmall)
            ResultadosBusquedaUi.NoDisponible -> Text(
                stringResource(Res.string.relevamientos_address_unavailable), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Mapa con la ubicación del relevamiento, su dirección si se consiguió y el botón para abrirla en el mapa del teléfono. */
@Composable
internal fun UbicacionDetalle(ubicacion: Ubicacion, direccion: DireccionUiState) {
    val uriHandler = LocalUriHandler.current
    CampoDetalle(stringResource(Res.string.relevamientos_location_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapaVistaPrevia(ubicacion)
            // Si la dirección no está disponible no se muestra nada: el mapa ya dice dónde es y no es un error.
            when (direccion) {
                DireccionUiState.Cargando -> Text(
                    stringResource(Res.string.relevamientos_address_loading), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("relevamientos_address_loading"),
                )
                is DireccionUiState.Disponible -> Text(
                    stringResource(Res.string.relevamientos_address, direccion.texto),
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("relevamientos_address_value"),
                )
                DireccionUiState.Ninguna, DireccionUiState.NoDisponible -> Unit
            }
            OutlinedButton(
                onClick = {
                    try {
                        uriHandler.openUri(enlaceMapa(ubicacion))
                    } catch (_: Exception) {
                        // Sin navegador ni app de mapas no hay otra forma de abrirlo; el mapa de la pantalla sigue a la vista.
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("relevamientos_location_open_map"),
            ) {
                IconoBoton(Iconos.OpenInNew)
                Text(stringResource(Res.string.relevamientos_location_open_map))
            }
        }
    }
}
