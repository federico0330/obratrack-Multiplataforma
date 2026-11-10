package com.unaj.obratrack.features.obras

import com.unaj.obratrack.core.ui.fechaLegible
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.IconoBoton
import com.unaj.obratrack.core.ui.Iconos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.EstadoObra
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Pestañas del detalle de la obra; el índice es el que se guarda para recordar dónde estaba el usuario. */
internal const val PESTANA_RESUMEN = 0
internal const val PESTANA_TAREAS = 1
internal const val PESTANA_RELEVAMIENTOS = 2

/**
 * Detalle de la obra: arriba, siempre visible, quién es y en qué estado está; debajo, tres pestañas (Resumen, Tareas y
 * Relevamientos) para que cada lista tenga su lugar y no se mezclen en un único desplazamiento largo.
 */
@Composable
internal fun DetalleObraContent(
    obra: Obra,
    state: ObrasUiState,
    onChangeStatus: (EstadoObra) -> Unit,
    onRequestDelete: () -> Unit,
    padding: PaddingValues,
    scrollState: ScrollState,
    pestana: Int,
    onPestanaChange: (Int) -> Unit,
    cantidadTareas: Int?,
    cantidadRelevamientos: Int?,
    tareasContent: @Composable () -> Unit,
    relevamientosContent: @Composable () -> Unit,
) {
    val ocupado = state.actualizando || state.eliminando
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(obra.nombre, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                EstadoChip(obra.estado)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painterResource(Iconos.Place), contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(obra.direccion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PrimaryTabRow(selectedTabIndex = pestana) {
            val titulos = listOf(
                stringResource(Res.string.obras_tab_summary),
                stringResource(Res.string.tareas_title),
                stringResource(Res.string.relevamientos_title),
            )
            val cantidades = listOf(null, cantidadTareas, cantidadRelevamientos)
            val tags = listOf("obras_tab_summary", "obras_tab_tareas", "obras_tab_relevamientos")
            titulos.forEachIndexed { i, titulo ->
                Tab(
                    selected = pestana == i,
                    onClick = {
                        if (pestana != i) {
                            onPestanaChange(i)
                            scope.launch { scrollState.scrollTo(0) }
                        }
                    },
                    text = { TituloPestana(titulo, cantidades[i]) },
                    modifier = Modifier.testTag(tags[i]),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (pestana) {
                PESTANA_TAREAS -> tareasContent()
                PESTANA_RELEVAMIENTOS -> relevamientosContent()
                else -> ResumenObra(obra, state, ocupado, onChangeStatus, onRequestDelete)
            }
            // Espacio para que el botón de crear, flotando abajo a la derecha, no tape la última tarjeta.
            if (pestana != PESTANA_RESUMEN) Spacer(Modifier.height(72.dp))
        }
    }
}

/** El número va arriba del nombre: "Relevamientos (5)" en una sola línea no entra en un tercio del ancho. */
@Composable
private fun TituloPestana(titulo: String, cantidad: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (cantidad != null) Text(cantidad.toString(), style = MaterialTheme.typography.titleSmall)
        Text(titulo, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun ResumenObra(
    obra: Obra,
    state: ObrasUiState,
    ocupado: Boolean,
    onChangeStatus: (EstadoObra) -> Unit,
    onRequestDelete: () -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dato(stringResource(Res.string.obras_client), obra.cliente, Modifier.weight(1f))
                Dato(
                    stringResource(Res.string.obras_start_date),
                    fechaLegible(obra.fechaInicio.toLocalDateTime(TimeZone.UTC).date),
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dato(
                    stringResource(Res.string.obras_last_visit),
                    obra.ultimaVisita?.let { fechaLegible(it.toLocalDateTime(TimeZone.UTC).date) } ?: "—",
                    Modifier.weight(1f),
                )
                Dato(
                    stringResource(Res.string.obras_visibility),
                    stringResource(if (obra.publica) Res.string.obras_public else Res.string.obras_private),
                    Modifier.weight(1f),
                )
            }
        }
    }
    state.errorOperacion?.let { ErrorBlock(errorMessage(it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (obra.estado) {
            EstadoObra.ACTIVA -> AccionEstado(Res.string.obras_pause, !ocupado, Modifier.weight(1f)) { onChangeStatus(EstadoObra.PAUSADA) }
            EstadoObra.PAUSADA, EstadoObra.FINALIZADA ->
                AccionEstado(Res.string.obras_reactivate, !ocupado, Modifier.weight(1f)) { onChangeStatus(EstadoObra.ACTIVA) }
        }
        if (obra.estado != EstadoObra.FINALIZADA) {
            AccionEstado(Res.string.obras_finish, !ocupado, Modifier.weight(1f)) { onChangeStatus(EstadoObra.FINALIZADA) }
        }
    }
    HorizontalDivider()
    OutlinedButton(
        onClick = onRequestDelete,
        enabled = !ocupado,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconoBoton(Iconos.Delete)
        Text(stringResource(Res.string.obras_delete))
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier) {
    Column(modifier) { CampoDetalle(etiqueta) { Text(valor, style = MaterialTheme.typography.bodyLarge) } }
}

@Composable
private fun AccionEstado(
    texto: org.jetbrains.compose.resources.StringResource,
    habilitado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, enabled = habilitado, modifier = modifier) {
        Text(stringResource(texto), maxLines = 2)
    }
}
