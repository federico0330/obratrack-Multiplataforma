package com.unaj.appmoviles.obratrack

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.unaj.obratrack.composeapp.ObraTrackApp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val proveedorTokenGoogle = (application as ObraTrackApplication).proveedorTokenGoogle
        proveedorTokenGoogle.adjuntar(this)
        setContent {
            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                ObraTrackApp(proveedorTokenGoogle, alAplicarTema = ::aplicarBarrasDelSistema)
            }
        }
    }

    // Con el tema forzado puede no coincidir con el del sistema: los íconos de las barras siguen al de la app.
    private fun aplicarBarrasDelSistema(oscuro: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(ESCRIM_CLARO, ESCRIM_OSCURO) { oscuro },
        )
    }

    private companion object {
        // Los mismos escrims que usa enableEdgeToEdge() por defecto.
        val ESCRIM_CLARO = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val ESCRIM_OSCURO = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
