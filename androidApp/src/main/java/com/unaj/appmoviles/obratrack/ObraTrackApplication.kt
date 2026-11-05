package com.unaj.appmoviles.obratrack

import android.app.Application

class ObraTrackApplication : Application() {
    /** default_web_client_id lo genera el plugin de google-services a partir de google-services.json. */
    val proveedorTokenGoogle by lazy { ProveedorTokenGoogleAndroid(getString(R.string.default_web_client_id)) }
}
