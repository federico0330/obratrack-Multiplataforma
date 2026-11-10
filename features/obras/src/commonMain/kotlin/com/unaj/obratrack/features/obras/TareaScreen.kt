package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.core.ui.Iconos
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pantalla del formulario de una tarea (alta o edición). Es una pantalla propia y no un tramo de la de la obra: acá no se
 * ve nada de la obra ni de sus relevamientos, así que cada botón es de la tarea. Se muestra mientras el formulario esté
 * abierto, y la flecha vuelve a la obra.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TareaScreen(state: TareasUiState, acciones: TareasAcciones, modifier: Modifier = Modifier) {
    val form = state.formulario
    BackHandler(enabled = true) { if (!form.guardando) acciones.onCloseForm() }
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(stringResource(if (form.tareaId == null) Res.string.tareas_create_title else Res.string.tareas_edit_title)) },
                    navigationIcon = {
                        IconButton(onClick = acciones.onCloseForm, enabled = !form.guardando, modifier = Modifier.testTag("tareas_back")) {
                            Icon(painter = painterResource(Iconos.ArrowBack), contentDescription = stringResource(Res.string.obras_back))
                        }
                    },
                )
                if (form.guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        bottomBar = { BarraGuardarTarea(form, acciones) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            FormularioTareaContent(form, acciones)
        }
    }
}
