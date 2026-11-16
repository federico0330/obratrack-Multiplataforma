package com.unaj.obratrack.features.obras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.domain.model.SugerenciaTarea
import com.unaj.obratrack.domain.model.TareaPlantilla
import com.unaj.obratrack.domain.model.TipoObra
import com.unaj.obratrack.domain.model.TituloSugerido
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Acciones de la pantalla de sugerencias; la pantalla no conoce al ViewModel. */
class SugerenciasAcciones(
    val onTipo: (TipoObra) -> Unit,
    val onDescripcion: (String) -> Unit,
    val onSugerirIa: () -> Unit,
    val onAlternar: (SugerenciaTarea) -> Unit,
    val onSeleccionarTodas: (Boolean) -> Unit,
    /** Recibe el título ya traducido de cada sugerencia: el ViewModel no conoce textos. */
    val onCrear: (Map<SugerenciaTarea, String>) -> Unit,
    val onClose: () -> Unit,
)

/**
 * Pantalla propia para elegir qué tareas crear a partir de un tipo de obra. La flecha vuelve a la obra y no se puede
 * usar mientras se guardan las tareas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun SugerenciasTareasScreen(state: SugerenciasUiState, acciones: SugerenciasAcciones, modifier: Modifier = Modifier) {
    BackHandler(enabled = true) { if (!state.creando) acciones.onClose() }
    val titulos = state.sugerencias.associateWith { tituloResuelto(it.titulo) }
    val error = state.error?.let { mensajeSugerencias(it) }
    val parcial = if (state.creadasAntesDelError > 0) stringResource(Res.string.sugerencias_partial, state.creadasAntesDelError) else null
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.sugerencias_title)) },
                    navigationIcon = {
                        IconButton(onClick = acciones.onClose, enabled = !state.creando) {
                            Icon(painter = painterResource(Iconos.ArrowBack), contentDescription = stringResource(Res.string.obras_back))
                        }
                    },
                )
                if (state.creando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            BarraGuardar(
                error = listOfNotNull(error, parcial).joinToString("\n").ifEmpty { null },
                guardando = state.creando,
                puedeGuardar = state.puedeCrear,
                textoGuardar = stringResource(Res.string.sugerencias_create, state.seleccionadas.size),
                textoCancelar = stringResource(if (state.altaDeObra) Res.string.sugerencias_skip else Res.string.obras_cancel),
                onGuardar = { acciones.onCrear(titulos) },
                onCancelar = acciones.onClose,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EtiquetaCampo(stringResource(Res.string.sugerencias_type_prompt))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoObra.entries.forEach { tipo ->
                    FilterChip(
                        selected = tipo == state.tipo,
                        onClick = { acciones.onTipo(tipo) },
                        enabled = !state.creando,
                        label = { Text(stringResource(tipo.etiqueta())) },
                    )
                }
            }
            if (state.iaDisponible) SugerirConIa(state, acciones)
            if (state.cargando) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            if (state.sugerencias.isNotEmpty()) ListaSugerencias(state, titulos, acciones)
        }
    }
}

@Composable
private fun SugerirConIa(state: SugerenciasUiState, acciones: SugerenciasAcciones) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.descripcion,
            onValueChange = acciones.onDescripcion,
            label = { Text(stringResource(Res.string.sugerencias_ia_label)) },
            placeholder = { Text(stringResource(Res.string.sugerencias_ia_placeholder)) },
            enabled = !state.creando && !state.cargando,
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = acciones.onSugerirIa, enabled = state.puedeSugerirConIa, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.sugerencias_ia_button))
        }
        Text(
            stringResource(Res.string.sugerencias_ia_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListaSugerencias(state: SugerenciasUiState, titulos: Map<SugerenciaTarea, String>, acciones: SugerenciasAcciones) {
    val todas = state.seleccionadas.size == state.sugerencias.size
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.sugerencias_select_prompt),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { acciones.onSeleccionarTodas(!todas) }, enabled = !state.creando) {
                Text(stringResource(if (todas) Res.string.sugerencias_select_none else Res.string.sugerencias_select_all))
            }
        }
        Text(
            stringResource(Res.string.sugerencias_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        state.sugerencias.forEach { sugerencia ->
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !state.creando) { acciones.onAlternar(sugerencia) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(checked = sugerencia in state.seleccionadas, onCheckedChange = null, enabled = !state.creando)
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(titulos.getValue(sugerencia), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        nombreCategoria(sugerencia.categoria),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun tituloResuelto(titulo: TituloSugerido): String = when (titulo) {
    is TituloSugerido.Plantilla -> stringResource(titulo.tarea.etiqueta())
    is TituloSugerido.Libre -> titulo.texto
}

@Composable
private fun mensajeSugerencias(error: ErrorSugerenciasUi): String = stringResource(
    when (error) {
        ErrorSugerenciasUi.SESION -> Res.string.sugerencias_error_session
        ErrorSugerenciasUi.RED -> Res.string.sugerencias_error_network
        ErrorSugerenciasUi.LIMITE -> Res.string.sugerencias_error_limit
        ErrorSugerenciasUi.RESPUESTA -> Res.string.sugerencias_error_response
        ErrorSugerenciasUi.DESCRIPCION -> Res.string.sugerencias_error_description
        ErrorSugerenciasUi.DESCONOCIDO -> Res.string.sugerencias_error_generic
    },
)

private fun TipoObra.etiqueta(): StringResource = when (this) {
    TipoObra.CASA_NUEVA -> Res.string.tipo_obra_casa_nueva
    TipoObra.AMPLIACION -> Res.string.tipo_obra_ampliacion
    TipoObra.REFORMA_INTEGRAL -> Res.string.tipo_obra_reforma_integral
    TipoObra.BANO -> Res.string.tipo_obra_bano
    TipoObra.COCINA -> Res.string.tipo_obra_cocina
    TipoObra.PINTURA -> Res.string.tipo_obra_pintura
}

private fun TareaPlantilla.etiqueta(): StringResource = when (this) {
    TareaPlantilla.PLANOS_Y_PERMISOS -> Res.string.plantilla_planos_y_permisos
    TareaPlantilla.LIMPIEZA_DEL_TERRENO -> Res.string.plantilla_limpieza_del_terreno
    TareaPlantilla.REPLANTEO -> Res.string.plantilla_replanteo
    TareaPlantilla.EXCAVACION -> Res.string.plantilla_excavacion
    TareaPlantilla.FUNDACIONES -> Res.string.plantilla_fundaciones
    TareaPlantilla.ESTRUCTURA_RESISTENTE -> Res.string.plantilla_estructura_resistente
    TareaPlantilla.MUROS -> Res.string.plantilla_muros
    TareaPlantilla.DEMOLICION -> Res.string.plantilla_demolicion
    TareaPlantilla.CUBIERTA -> Res.string.plantilla_cubierta
    TareaPlantilla.INSTALACION_ELECTRICA -> Res.string.plantilla_instalacion_electrica
    TareaPlantilla.INSTALACION_SANITARIA -> Res.string.plantilla_instalacion_sanitaria
    TareaPlantilla.INSTALACION_GAS -> Res.string.plantilla_instalacion_gas
    TareaPlantilla.IMPERMEABILIZACION -> Res.string.plantilla_impermeabilizacion
    TareaPlantilla.REVOQUES -> Res.string.plantilla_revoques
    TareaPlantilla.CONTRAPISOS -> Res.string.plantilla_contrapisos
    TareaPlantilla.PISOS -> Res.string.plantilla_pisos
    TareaPlantilla.REVESTIMIENTOS -> Res.string.plantilla_revestimientos
    TareaPlantilla.ABERTURAS -> Res.string.plantilla_aberturas
    TareaPlantilla.CARPINTERIA_INTERIOR -> Res.string.plantilla_carpinteria_interior
    TareaPlantilla.MUEBLES_A_MEDIDA -> Res.string.plantilla_muebles_a_medida
    TareaPlantilla.HERRERIA -> Res.string.plantilla_herreria
    TareaPlantilla.CLIMATIZACION -> Res.string.plantilla_climatizacion
    TareaPlantilla.PINTURA_INTERIOR -> Res.string.plantilla_pintura_interior
    TareaPlantilla.PINTURA_EXTERIOR -> Res.string.plantilla_pintura_exterior
    TareaPlantilla.ARTEFACTOS_Y_GRIFERIA -> Res.string.plantilla_artefactos_y_griferia
    TareaPlantilla.ARTEFACTOS_DE_ILUMINACION -> Res.string.plantilla_artefactos_de_iluminacion
    TareaPlantilla.RETIRO_DE_ESCOMBROS -> Res.string.plantilla_retiro_de_escombros
    TareaPlantilla.LIMPIEZA_FINAL -> Res.string.plantilla_limpieza_final
    TareaPlantilla.RECEPCION_Y_ENTREGA -> Res.string.plantilla_recepcion_y_entrega
}
