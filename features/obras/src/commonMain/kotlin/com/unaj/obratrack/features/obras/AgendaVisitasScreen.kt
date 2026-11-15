package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.ErrorBlock
import com.unaj.obratrack.core.ui.fechaLegible
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.usecase.CategoriaAgendaVisita
import com.unaj.obratrack.domain.usecase.agendaVisitas
import com.unaj.obratrack.features.obras.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaVisitasScreen(
    lista: ListaObrasUiState,
    edicion: EdicionVisitaUiState?,
    onRetry: () -> Unit,
    onEdit: (Obra) -> Unit,
    onCloseEditor: () -> Unit,
    onSave: (LocalDate?, LocalTime?) -> Unit,
    clock: Clock = Clock.System,
) {
    if (edicion != null) {
        EdicionVisitaDialog(edicion, onCloseEditor, onSave)
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.agenda_title)) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).testTag("agenda_visitas")) {
            when (lista) {
                ListaObrasUiState.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ListaObrasUiState.Error -> Column(Modifier.padding(16.dp)) {
                    ErrorBlock(errorMessage(lista.motivo))
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.obras_retry)) }
                }
                ListaObrasUiState.Vacia -> AgendaVacia()
                is ListaObrasUiState.Contenido -> {
                    val ahora by produceState(clock.now(), clock) {
                        while (true) {
                            value = clock.now()
                            delay(60_000)
                        }
                    }
                    val grupos = agendaVisitas(lista.obras, ahora, TimeZone.currentSystemDefault()).groupBy { it.categoria }
                    if (grupos.isEmpty()) {
                        AgendaVacia()
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((categoria, visitas) in grupos) {
                                item {
                                    Text(stringResource(when (categoria) {
                                        CategoriaAgendaVisita.VENCIDAS -> Res.string.agenda_overdue
                                        CategoriaAgendaVisita.HOY -> Res.string.agenda_today
                                        CategoriaAgendaVisita.ESTA_SEMANA -> Res.string.agenda_week
                                        CategoriaAgendaVisita.PROXIMAS -> Res.string.agenda_later
                                        CategoriaAgendaVisita.SIN_PROGRAMAR -> Res.string.agenda_unscheduled
                                    }), style = MaterialTheme.typography.titleMedium)
                                }
                                items(visitas, key = { it.obra.id }) { visita ->
                                    OutlinedCard(Modifier.fillMaxWidth().testTag("agenda_obra_${visita.obra.id}")) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(visita.obra.nombre, style = MaterialTheme.typography.titleMedium)
                                            visita.obra.direccion?.let { Text(it) }
                                            val fechaVisita = visita.fecha
                                            if (fechaVisita != null) {
                                                val hora = visita.horaMinutos?.let(::horaVisitaTexto)
                                                val fecha = fechaLegible(fechaVisita)
                                                Text(if (hora == null) fecha else stringResource(Res.string.agenda_date_time, fecha, hora))
                                            }
                                            visita.diasSinVisita?.toInt()?.let {
                                                Text(pluralStringResource(Res.plurals.agenda_days_without_visit, it, it))
                                            }
                                            TextButton(
                                                onClick = { onEdit(visita.obra) },
                                                modifier = Modifier.testTag("agenda_editar_${visita.obra.id}"),
                                            ) {
                                                val accion = if (fechaVisita == null) Res.string.agenda_schedule else Res.string.agenda_reschedule
                                                Text(stringResource(accion))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaVacia() {
    Text(stringResource(Res.string.agenda_empty), Modifier.padding(16.dp))
}
