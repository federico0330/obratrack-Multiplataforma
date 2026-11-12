package com.unaj.obratrack.composeapp

import com.unaj.obratrack.core.data.fotos.CloudinaryConfig
import com.unaj.obratrack.core.data.fotos.crearAlmacenFotosCloudinary
import com.unaj.obratrack.core.data.relevamientos.RepositorioRelevamientosImpl
import com.unaj.obratrack.core.data.direcciones.crearProveedorDireccionesCompass
import com.unaj.obratrack.core.data.ubicacion.crearProveedorUbicacionCompass
import com.unaj.obratrack.core.firebase.relevamientos.GitLiveFirebaseRelevamientoDataSource
import com.unaj.obratrack.domain.usecase.ActualizarRelevamiento
import com.unaj.obratrack.domain.usecase.EliminarRelevamiento
import com.unaj.obratrack.domain.usecase.CrearRelevamiento
import com.unaj.obratrack.domain.usecase.ObservarRelevamientos
import com.unaj.obratrack.domain.usecase.BuscarDirecciones
import com.unaj.obratrack.domain.usecase.ObtenerDireccion
import com.unaj.obratrack.domain.usecase.ObtenerUbicacionActual
import com.unaj.obratrack.features.obras.RelevamientosViewModel
import com.unaj.obratrack.core.data.auth.RepositorioAutenticacionImpl
import com.unaj.obratrack.core.data.auth.RepositorioCuentaImpl
import com.unaj.obratrack.core.data.preferencias.crearRepositorioPreferencias
import com.unaj.obratrack.core.data.sugerencias.GeminiConfig
import com.unaj.obratrack.core.data.sugerencias.SugeridorPorPlantillas
import com.unaj.obratrack.core.data.sugerencias.crearSugeridorConGemini
import com.unaj.obratrack.domain.usecase.CrearTareasSugeridas
import com.unaj.obratrack.domain.usecase.SugerirTareas
import com.unaj.obratrack.domain.usecase.SugerirTareasConIa
import com.unaj.obratrack.features.obras.SugerenciasViewModel
import com.unaj.obratrack.domain.usecase.ActualizarNombreUsuario
import com.unaj.obratrack.domain.usecase.CambiarIdioma
import com.unaj.obratrack.domain.usecase.CambiarTema
import com.unaj.obratrack.domain.usecase.ObservarIdioma
import com.unaj.obratrack.domain.usecase.ObservarTema
import com.unaj.obratrack.domain.usecase.RecuperarContrasena
import com.unaj.obratrack.features.auth.CuentaViewModel
import com.unaj.obratrack.features.auth.PreferenciasViewModel
import com.unaj.obratrack.core.data.obras.RepositorioObrasImpl
import com.unaj.obratrack.core.data.tareas.RepositorioTareasImpl
import com.unaj.obratrack.core.firebase.auth.GitLiveFirebaseAuthDataSource
import com.unaj.obratrack.core.firebase.obras.GitLiveFirebaseObraDataSource
import com.unaj.obratrack.core.firebase.tareas.GitLiveFirebaseTareaDataSource
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
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
import com.unaj.obratrack.domain.usecase.IniciarSesionConGoogle
import com.unaj.obratrack.domain.usecase.ObservarObras
import com.unaj.obratrack.domain.usecase.ObservarSesion
import com.unaj.obratrack.domain.usecase.RegistrarUsuario
import com.unaj.obratrack.domain.usecase.ProgramarVisita
import com.unaj.obratrack.features.auth.AuthViewModel
import com.unaj.obratrack.features.auth.PerfilViewModel
import com.unaj.obratrack.features.auth.RegistroViewModel
import com.unaj.obratrack.features.obras.ObrasViewModel
import com.unaj.obratrack.features.obras.TareasViewModel

/**
 * Composition root compartido; cada plataforma inicializa Firebase antes de crear este grafo y
 * aporta su implementación de [ProveedorTokenGoogle].
 */
class AppGraph(
    private val proveedorTokenGoogle: ProveedorTokenGoogle,
    claveGemini: String = "",
    modeloGemini: String = "",
) {
    private val authDataSource = GitLiveFirebaseAuthDataSource()
    private val authRepository = RepositorioAutenticacionImpl(authDataSource)
    private val cuentaRepository = RepositorioCuentaImpl(authDataSource)
    private val preferenciasRepository = crearRepositorioPreferencias()
    private val sugeridorTareas = SugeridorPorPlantillas()

    // Sin clave no hay sugerencias con IA: la pantalla ofrece solo las plantillas.
    private val sugeridorIa = claveGemini.takeIf { it.isNotBlank() }?.let {
        crearSugeridorConGemini(GeminiConfig(it, modeloGemini.ifBlank { GeminiConfig.MODELO_POR_DEFECTO }))
    }
    private val obrasRepository = RepositorioObrasImpl(GitLiveFirebaseObraDataSource())
    private val tareasRepository = RepositorioTareasImpl(GitLiveFirebaseTareaDataSource())

    private val relevamientosRepository = RepositorioRelevamientosImpl(GitLiveFirebaseRelevamientoDataSource())

    // Misma cuenta y preset sin firma que la app Android anterior: las fotos de ambas quedan juntas.
    private val almacenFotos = crearAlmacenFotosCloudinary(
        CloudinaryConfig(cloudName = "iwsier4h", uploadPreset = "obratrack_unsigned"),
    )

    // Compass se crea recién al primer pedido de ubicación o de dirección.
    private val proveedorUbicacion = crearProveedorUbicacionCompass()
    private val proveedorDirecciones = crearProveedorDireccionesCompass()

    fun crearRelevamientosViewModel() = RelevamientosViewModel(
        ObservarSesion(authRepository), ObservarRelevamientos(relevamientosRepository),
        CrearRelevamiento(relevamientosRepository, almacenFotos), ObservarTareas(tareasRepository),
        ActualizarRelevamiento(relevamientosRepository, almacenFotos), EliminarRelevamiento(relevamientosRepository),
        ObtenerUbicacionActual(proveedorUbicacion), ObtenerDireccion(proveedorDirecciones), BuscarDirecciones(proveedorDirecciones),
    )

    fun crearAuthViewModel() = AuthViewModel(
        IniciarSesion(authRepository),
        ObservarSesion(authRepository),
        CerrarSesion(authRepository),
        IniciarSesionConGoogle(proveedorTokenGoogle, authRepository),
    )

    fun crearRegistroViewModel() = RegistroViewModel(RegistrarUsuario(authRepository))

    fun crearCuentaViewModel() = CuentaViewModel(
        RecuperarContrasena(cuentaRepository),
        ActualizarNombreUsuario(cuentaRepository),
    )

    fun crearPreferenciasViewModel() = PreferenciasViewModel(
        ObservarTema(preferenciasRepository),
        CambiarTema(preferenciasRepository),
        ObservarIdioma(preferenciasRepository),
        CambiarIdioma(preferenciasRepository),
    )

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
        ProgramarVisita(obrasRepository),
    )

    fun crearSugerenciasViewModel() = SugerenciasViewModel(
        ObservarSesion(authRepository),
        SugerirTareas(sugeridorTareas),
        CrearTareasSugeridas(CrearTarea(tareasRepository)),
        sugeridorIa?.let { SugerirTareasConIa(it) },
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
