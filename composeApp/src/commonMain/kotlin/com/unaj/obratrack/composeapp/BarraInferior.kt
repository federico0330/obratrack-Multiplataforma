package com.unaj.obratrack.composeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.composeapp.generated.resources.Res
import com.unaj.obratrack.composeapp.generated.resources.nav_obras
import com.unaj.obratrack.composeapp.generated.resources.nav_perfil
import org.jetbrains.compose.resources.stringResource

enum class PestanaPrincipal { OBRAS, PERFIL }

/** Guarda la pestaña por nombre: un enum no se puede guardar directamente en todas las plataformas. */
val PestanaPrincipalSaver: Saver<PestanaPrincipal, String> = Saver(
    save = { it.name },
    restore = { PestanaPrincipal.valueOf(it) },
)

/**
 * Barra inferior sin estado: recibe la pestaña activa y emite la elegida. Solo texto, porque
 * los íconos de Material requieren `materialIconsExtended`, que no se suma a este módulo.
 */
@Composable
fun BarraInferior(
    seleccionada: PestanaPrincipal,
    onSeleccionar: (PestanaPrincipal) -> Unit,
) {
    NavigationBar {
        ItemTexto(
            texto = stringResource(Res.string.nav_obras),
            seleccionado = seleccionada == PestanaPrincipal.OBRAS,
            onClick = { onSeleccionar(PestanaPrincipal.OBRAS) },
        )
        ItemTexto(
            texto = stringResource(Res.string.nav_perfil),
            seleccionado = seleccionada == PestanaPrincipal.PERFIL,
            onClick = { onSeleccionar(PestanaPrincipal.PERFIL) },
        )
    }
}

@Composable
private fun RowScope.ItemTexto(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = seleccionado, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(3.dp)
                .background(
                    color = if (seleccionado) colores.primary else Color.Transparent,
                    shape = MaterialTheme.shapes.extraSmall,
                ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
            color = if (seleccionado) colores.primary else colores.onSurfaceVariant,
        )
    }
}
