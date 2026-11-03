package com.unaj.obratrack.composeapp

import com.unaj.obratrack.core.data.auth.RepositorioAutenticacionImpl
import com.unaj.obratrack.core.data.obras.RepositorioObrasImpl
import com.unaj.obratrack.core.data.tareas.RepositorioTareasImpl
import com.unaj.obratrack.core.firebase.auth.GitLiveFirebaseAuthDataSource
import com.unaj.obratrack.core.firebase.obras.GitLiveFirebaseObraDataSource
import com.unaj.obratrack.core.firebase.tareas.GitLiveFirebaseTareaDataSource
import com.unaj.obratrack.domain.usecase.ActualizarObra
import com.unaj.obratrack.domain.usecase.ActualizarTarea
import com.unaj.obratrack.domain.usecase.CambiarEstadoObra
import com.unaj.obratrack.domain.usecase.CambiarEstadoTarea
import com.unaj.obratrack.domain.usecase.CrearTarea
import com.unaj.obratrack.domain.usecase.EliminarTarea
import com.unaj.obratrack.domain.usecase.ObservarTareas
import com.unaj.obratrack.domain.usecase.CerrarSesion
import com.unaj.obratrack.domain.usecase.CrearObra
import com.unaj.obratrack.domain.usecase.EliminarObra
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.RegistrarUsuario
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.PerfilViewModel
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.obras.ObrasViewModel
import com.unaj.obratrack.features.obras.TareasViewModel

/** Composition root compartido; cada plataforma inicializa Firebase antes de crear este grafo. */
class AppGraph {
    private val authRepository = RepositorioAutenticacionImpl(GitLiveFirebaseAuthDataSource())
    private val obrasRepository = RepositorioObrasImpl(GitLiveFirebaseObraDataSource())
    private val tareasRepository = RepositorioTareasImpl(GitLiveFirebaseTareaDataSource())

    fun crearAuthViewModel() = AuthViewModel(
        IniciarSesion(authRepository),
        ObservarSesion(authRepository),
        CerrarSesion(authRepository),
    )

    fun crearRegistroViewModel() = RegistroViewModel(RegistrarUsuario(authRepository))

    fun crearPerfilViewModel() = PerfilViewModel(
        ObservarSesion(authRepository),
        ObservarObras(obrasRepository),
    )

    fun crearObrasViewModel() = ObrasViewModel(
        ObservarSesion(authRepository),
        ObservarObras(obrasRepository),
        CrearObra(obrasRepository),
        ActualizarObra(obrasRepository),
        CambiarEstadoObra(obrasRepository),
        EliminarObra(obrasRepository),
    )

    fun crearTareasViewModel() = TareasViewModel(
        ObservarSesion(authRepository),
        ObservarTareas(tareasRepository),
        CrearTarea(tareasRepository),
        ActualizarTarea(tareasRepository),
        CambiarEstadoTarea(tareasRepository),
        EliminarTarea(tareasRepository),
    )
}
