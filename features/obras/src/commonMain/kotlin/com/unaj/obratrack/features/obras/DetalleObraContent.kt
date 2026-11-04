package com.unaj.obratrack.features.obras

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetalleObraContent(
    obra: Obra,
    state: ObrasUiState,
    onEditObra: () -> Unit,
    onChangeStatus: (EstadoObra) -> Unit,
    onRequestDelete: () -> Unit,
    padding: PaddingValues,
    detailExtra: @Composable () -> Unit = {},
) {
    val ocupado = state.actualizando || state.eliminando
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(obra.nombre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            EstadoChip(obra.estado)
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.obras_client_value, obra.cliente))
                Text(obra.direccion)
                Text(
                    stringResource(
                        Res.string.obras_start_date_value,
                        obra.fechaInicio.toLocalDateTime(TimeZone.UTC).date.toString(),
                    ),
                )
                Text(
                    stringResource(
                        Res.string.obras_visibility_value,
                        stringResource(if (obra.publica) Res.string.obras_public else Res.string.obras_private),
                    ),
                )
            }
        }
        state.errorOperacion?.let { ErrorBlock(errorMessage(it)) }
        Button(onClick = onEditObra, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
            IconoBoton(Res.drawable.ic_edit)
            Text(stringResource(Res.string.obras_edit))
        }
        when (obra.estado) {
            EstadoObra.ACTIVA -> AccionEstado(Res.string.obras_pause, !ocupado) { onChangeStatus(EstadoObra.PAUSADA) }
            EstadoObra.PAUSADA, EstadoObra.FINALIZADA ->
                AccionEstado(Res.string.obras_reactivate, !ocupado) { onChangeStatus(EstadoObra.ACTIVA) }
        }
        if (obra.estado != EstadoObra.FINALIZADA) {
            AccionEstado(Res.string.obras_finish, !ocupado) { onChangeStatus(EstadoObra.FINALIZADA) }
        }
        detailExtra()
        HorizontalDivider()
        OutlinedButton(
            onClick = onRequestDelete,
            enabled = !ocupado,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconoBoton(Res.drawable.ic_delete)
            Text(stringResource(Res.string.obras_delete))
        }
    }
}

@Composable
private fun AccionEstado(
    texto: org.jetbrains.compose.resources.StringResource,
    habilitado: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, enabled = habilitado, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(texto))
    }
}
