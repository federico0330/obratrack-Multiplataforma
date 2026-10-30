package com.unaj.appmoviles.obratrack

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unaj.obratrack.core.data.auth.RepositorioAutenticacionImpl
import com.unaj.obratrack.core.firebase.auth.GitLiveFirebaseAuthDataSource
import com.unaj.obratrack.domain.usecase.CerrarSesion
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.features.auth.AuthViewModel

/** Composition root: conecta implementaciones y las inyecta por constructor. */
class ObraTrackApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}

class AppContainer {
    private val remoteAuth = GitLiveFirebaseAuthDataSource()
    private val authRepository = RepositorioAutenticacionImpl(remoteAuth)
    private val login = IniciarSesion(authRepository)
    private val observarSesion = ObservarSesion(authRepository)
    private val cerrarSesion = CerrarSesion(authRepository)

    val authViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            return AuthViewModel(login, observarSesion, cerrarSesion) as T
        }
    }
}
