package com.unaj.appmoviles.obratrack

import android.os.Bundle
import androidx.activity.ComponentActivity
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
            Box(Modifier.semantics { testTagsAsResourceId = true }) { ObraTrackApp(proveedorTokenGoogle) }
        }
    }
}
