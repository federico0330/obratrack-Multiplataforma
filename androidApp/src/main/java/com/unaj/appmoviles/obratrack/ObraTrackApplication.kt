package com.unaj.appmoviles.obratrack

import android.app.Application

class ObraTrackApplication : Application() {
    /** default_web_client_id lo genera el plugin de google-services a partir de google-services.json. */
    val proveedorTokenGoogle by lazy { ProveedorTokenGoogleAndroid(getString(R.string.default_web_client_id)) }

    /** Avisos locales de visitas; lazy para no tocar WorkManager hasta que la app lo use. */
    val programadorAvisos by lazy { ProgramadorAvisosAndroid(this) }
}
