package com.unaj.obratrack.features.obras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Obra
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.features.obras.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ListaObrasContent(
    user: Usuario,
    lista: ListaObrasUiState,
    onRetry: () -> Unit,
    onSelectObra: (Obra) -> Unit,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        // El margen inferior extra evita que el botón flotante tape la última tarjeta.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.obras_signed_in, user.nombre?.takeIf(String::isNotBlank) ?: user.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (lista) {
            ListaObrasUiState.Cargando -> item {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ListaObrasUiState.Vacia -> item { EstadoVacio() }
            is ListaObrasUiState.Contenido -> items(lista.obras, key = { it.id }) { obra ->
                ObraCard(obra, onSelectObra)
            }
            is ListaObrasUiState.Error -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorBlock(errorMessage(lista.motivo))
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.obras_retry)) }
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.obras_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.obras_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
