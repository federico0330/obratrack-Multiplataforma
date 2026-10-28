package com.unaj.appmoviles.obratrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.unaj.obratrack.data.auth.FirebaseRepositorioAutenticacion
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.ResultadoLogin
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ObraTrackApp() }
    }
}

@Composable
private fun ObraTrackApp() {
    val repository = remember { FirebaseRepositorioAutenticacion() }
    val user by repository.usuarioActual.collectAsState(initial = null)
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (user == null) LoginScreen(IniciarSesion(repository))
            else HomeScreen(user!!, repository::cerrarSesion)
        }
    }
}

@Composable
private fun LoginScreen(login: IniciarSesion) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ObraTrack", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Ingresá para continuar", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        message?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    message = when (login(email, password)) {
                        is ResultadoLogin.Exito -> null
                        ResultadoLogin.DatosInvalidos -> "Revisá el correo y la contraseña ingresados."
                        ResultadoLogin.CredencialesInvalidas -> "El correo o la contraseña no son correctos."
                        ResultadoLogin.ErrorDeRed -> "No hay conexión. Revisá tu internet e intentá de nuevo."
                        ResultadoLogin.ErrorInesperado -> "No pudimos iniciar sesión. Intentá nuevamente."
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator() else Text("Iniciar sesión")
        }
    }
}

@Composable
private fun HomeScreen(user: Usuario, logout: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
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
        TextButton(onClick = { scope.launch { logout() } }) { Text("Cerrar sesión") }
    }
}
