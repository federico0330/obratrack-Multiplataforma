package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.ErrorBlock

/*
 * Piezas comunes de las pantallas propias (relevamientos y tareas): el mismo título de dato, de bloque y de barra de
 * guardado, para que todos los formularios y detalles se lean igual.
 */

/** Título chico de un dato: siempre igual en el detalle y en el formulario, para que se lea de un vistazo. */
@Composable
internal fun EtiquetaCampo(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Un dato del detalle: su título y debajo el valor. */
@Composable
internal fun CampoDetalle(etiqueta: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EtiquetaCampo(etiqueta)
        contenido()
    }
}

/** Un bloque del formulario (Datos, Foto, Ubicación, Cronograma) con su título. */
@Composable
internal fun SeccionFormulario(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        contenido()
    }
}

/**
 * Barra fija abajo de un formulario: el error (para que se vea sin desplazar la pantalla) y los botones de guardar y
 * cancelar. Sube con el teclado, así que no queda tapada ni al final de un formulario largo.
 *
 * [puedeGuardar] permite bloquear Guardar por algo más que el guardado en curso (por ejemplo, una búsqueda pendiente).
 */
@Composable
internal fun BarraGuardar(
    error: String?,
    guardando: Boolean,
    puedeGuardar: Boolean,
    textoGuardar: String,
    textoCancelar: String,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    tagGuardar: String? = null,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 3.dp, modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            error?.let { ErrorBlock(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGuardar, enabled = !guardando && puedeGuardar,
                    modifier = Modifier.weight(1f).then(if (tagGuardar != null) Modifier.testTag(tagGuardar) else Modifier),
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(textoGuardar)
                }
                OutlinedButton(onClick = onCancelar, enabled = !guardando, modifier = Modifier.weight(1f)) { Text(textoCancelar) }
            }
        }
    }
}
