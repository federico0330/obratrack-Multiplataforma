package com.unaj.appmoviles.obratrack

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.unaj.obratrack.composeapp.ObraTrackApp
import com.unaj.obratrack.composeapp.idiomaGuardado
import com.unaj.obratrack.domain.model.IdiomaApp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import java.util.Locale

class MainActivity : ComponentActivity() {
    // Idioma con el que se creó esta actividad; si el guardado cambia, se vuelve a crear.
    private var idiomaAplicado = IdiomaApp.SISTEMA

    override fun attachBaseContext(base: Context) {
        idiomaAplicado = idiomaGuardado()
        super.attachBaseContext(base.conIdioma(idiomaAplicado))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val proveedorTokenGoogle = (application as ObraTrackApplication).proveedorTokenGoogle
        proveedorTokenGoogle.adjuntar(this)
        setContent {
            Box(Modifier.semantics { testTagsAsResourceId = true }) {
                ObraTrackApp(
                    proveedorTokenGoogle,
                    alAplicarTema = ::aplicarBarrasDelSistema,
                    alAplicarIdioma = ::aplicarIdioma,
                )
            }
        }
    }

    private fun aplicarIdioma(idioma: IdiomaApp) {
        if (idioma != idiomaAplicado) recreate()
    }

    // Con el tema forzado puede no coincidir con el del sistema: los íconos de las barras siguen al de la app.
    private fun aplicarBarrasDelSistema(oscuro: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(ESCRIM_CLARO, ESCRIM_OSCURO) { oscuro },
        )
    }

    /**
     * Fija el idioma de la aplicación. Con el del sistema se restablece el locale del dispositivo,
     * porque dentro del mismo proceso el locale por defecto pudo haber quedado en el elegido antes.
     */
    private fun Context.conIdioma(idioma: IdiomaApp): Context {
        val locale = idioma.codigo?.let(Locale::forLanguageTag)
            ?: Resources.getSystem().configuration.locales[0]
        Locale.setDefault(locale)
        val configuracion = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(configuracion)
    }

    private companion object {
        // Los mismos escrims que usa enableEdgeToEdge() por defecto.
        val ESCRIM_CLARO = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val ESCRIM_OSCURO = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
