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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.domain.model.Usuario

@Composable
fun HomeScreen(user: Usuario, onSignOut: () -> Unit, errorMessage: String? = null) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_signed_in_as, user.nombre?.takeIf(String::isNotBlank) ?: user.email))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.home_empty_hint))
        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.home_sign_out)) }
    }
}
