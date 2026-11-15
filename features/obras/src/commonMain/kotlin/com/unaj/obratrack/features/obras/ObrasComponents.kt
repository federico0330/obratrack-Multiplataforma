package com.unaj.obratrack.features.obras

import com.unaj.obratrack.core.ui.Etiqueta
import com.unaj.obratrack.core.ui.Iconos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Etiqueta con el estado de la obra, coloreada según el estado. */
@Composable
internal fun EstadoChip(estado: EstadoObra, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (estado) {
        EstadoObra.ACTIVA -> colors.primaryContainer to colors.onPrimaryContainer
        EstadoObra.PAUSADA -> colors.secondaryContainer to colors.onSecondaryContainer
        EstadoObra.FINALIZADA -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    val icono = when (estado) {
        EstadoObra.ACTIVA -> Iconos.PlayCircle
        EstadoObra.PAUSADA -> Iconos.PauseCircle
        EstadoObra.FINALIZADA -> Iconos.CheckCircle
    }
    Etiqueta(estadoNombre(estado), icono, container, content, modifier)
}

@Composable
internal fun ObraCard(obra: Obra, onClick: (Obra) -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = { onClick(obra) }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(obra.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                EstadoChip(obra.estado)
            }
            obra.cliente?.let { cliente ->
                Text(
                    stringResource(Res.string.obras_client_value, cliente),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            obra.direccion?.let { direccion ->
                Text(
                    direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun estadoNombre(estado: EstadoObra): String = stringResource(
    when (estado) {
        EstadoObra.ACTIVA -> Res.string.obras_state_active
        EstadoObra.PAUSADA -> Res.string.obras_state_paused
        EstadoObra.FINALIZADA -> Res.string.obras_state_finished
    },
)

@Composable
internal fun errorMessage(error: ErrorObrasUi): String = stringResource(
    when (error) {
        ErrorObrasUi.SESION, ErrorObrasUi.PERMISOS -> Res.string.obras_error_session
        ErrorObrasUi.NOMBRE -> Res.string.obras_error_name
        ErrorObrasUi.FECHA -> Res.string.obras_error_date
        ErrorObrasUi.RED -> Res.string.obras_error_network
        ErrorObrasUi.BAJA_PARCIAL -> Res.string.obras_error_partial_delete
        ErrorObrasUi.DESCONOCIDO -> Res.string.obras_error_generic
    },
)
