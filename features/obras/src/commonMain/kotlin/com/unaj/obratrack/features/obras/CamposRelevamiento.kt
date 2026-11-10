package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

/** Un bloque del formulario (Datos, Foto, Ubicación) con su título. */
@Composable
internal fun SeccionFormulario(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        contenido()
    }
}
