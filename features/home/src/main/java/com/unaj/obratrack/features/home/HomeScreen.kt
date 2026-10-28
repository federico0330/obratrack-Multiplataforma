package com.unaj.obratrack.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Usuario

@Composable
fun HomeScreen(user: Usuario, onSignOut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Mis obras", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Sesión iniciada como ${user.nombre?.takeIf(String::isNotBlank) ?: user.email}")
        Spacer(Modifier.height(24.dp))
        Text("Todavía no hay obras para mostrar.", style = MaterialTheme.typography.bodyLarge)
        Text("Esta será la vista principal para consultar tus proyectos.")
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
    }
}
