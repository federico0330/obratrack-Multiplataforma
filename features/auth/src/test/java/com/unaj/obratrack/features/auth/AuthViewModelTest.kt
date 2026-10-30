package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorAuth
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.usecase.CerrarSesion
import com.unaj.obratrack.domain.usecase.IniciarSesion
import com.unaj.obratrack.domain.usecase.ObservarSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val usuario = Usuario("u1", "persona@ejemplo.com", null)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun alIniciarSeChequeaLaSesionSinUsuario() = runTest {
        val viewModel = viewModelCon(RepositorioFalso())

        assertTrue(viewModel.state.value.sessionChecked)
        assertNull(viewModel.state.value.user)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun alIniciarConSesionExistenteHayUsuario() = runTest {
        val viewModel = viewModelCon(RepositorioFalso(sesion = usuario))

        assertEquals(usuario, viewModel.state.value.user)
    }

    @Test
    fun loginExitosoDejaUsuarioYSinError() = runTest {
        val viewModel = viewModelCon(RepositorioFalso(resultadoAcceso = ResultadoAcceso.Exito(usuario)))

        completarYIngresar(viewModel)

        assertEquals(usuario, viewModel.state.value.user)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun credencialesInvalidasLlegaAlEstado() = loginFallido(ErrorAuth.CredencialesInvalidas, AuthError.InvalidCredentials)

    @Test
    fun sinConexionLlegaAlEstado() = loginFallido(ErrorAuth.SinConexion, AuthError.NoConnection)

    @Test
    fun demasiadosIntentosLlegaAlEstado() = loginFallido(ErrorAuth.DemasiadosIntentos, AuthError.TooManyAttempts)

    @Test
    fun usuarioDeshabilitadoLlegaAlEstado() = loginFallido(ErrorAuth.UsuarioDeshabilitado, AuthError.UserDisabled)

    @Test
    fun errorInesperadoLlegaAlEstado() = loginFallido(ErrorAuth.Inesperado, AuthError.Unexpected)

    @Test
    fun correoSinArrobaMuestraDatosInvalidos() = runTest {
        val repositorio = RepositorioFalso()
        val viewModel = viewModelCon(repositorio)

        viewModel.onEmailChanged("sin-arroba")
        viewModel.onPasswordChanged("clave")
        viewModel.signIn()

        assertEquals(AuthError.InvalidData, viewModel.state.value.error)
        assertEquals(0, repositorio.loginCalls)
    }

    @Test
    fun noPermiteIngresarConCamposVacios() = runTest {
        val repositorio = RepositorioFalso()
        val viewModel = viewModelCon(repositorio)

        assertFalse(viewModel.state.value.canSignIn)
        viewModel.signIn()
        viewModel.onEmailChanged("persona@ejemplo.com")
        assertFalse(viewModel.state.value.canSignIn)
        viewModel.signIn()
        viewModel.onPasswordChanged("   ")
        assertFalse(viewModel.state.value.canSignIn)
        viewModel.signIn()

        assertEquals(0, repositorio.loginCalls)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun escribirLimpiaElError() = runTest {
        val viewModel = viewModelCon(
            RepositorioFalso(resultadoAcceso = ResultadoAcceso.Fallo(ErrorAuth.SinConexion)),
        )
        completarYIngresar(viewModel)
        assertEquals(AuthError.NoConnection, viewModel.state.value.error)

        viewModel.onPasswordChanged("otra")

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun cierreDeSesionExitosoQuitaElUsuario() = runTest {
        val viewModel = viewModelCon(RepositorioFalso(sesion = usuario))

        viewModel.signOut()

        assertNull(viewModel.state.value.user)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun cierreDeSesionFallidoMuestraErrorYMantieneElUsuario() = runTest {
        val viewModel = viewModelCon(
            RepositorioFalso(
                sesion = usuario,
                resultadoCerrarSesion = ResultadoCerrarSesion.Fallo(ErrorAuth.Inesperado),
            ),
        )

        viewModel.signOut()

        assertEquals(AuthError.SignOutFailed, viewModel.state.value.error)
        assertEquals(usuario, viewModel.state.value.user)
    }

    @Test
    fun fallaDelFlowDeSesionTerminaElChequeoYMuestraError() = runTest {
        val viewModel = viewModelCon(RepositorioFalso(fallasDelFlow = 1))

        assertTrue(viewModel.state.value.sessionChecked)
        assertNull(viewModel.state.value.user)
        assertEquals(AuthError.SessionFailed, viewModel.state.value.error)
    }

    @Test
    fun trasFallaDelFlowUnLoginExitosoLlevaAlUsuario() = runTest {
        val repositorio = RepositorioFalso(
            resultadoAcceso = ResultadoAcceso.Exito(usuario),
            fallasDelFlow = 1,
        )
        val viewModel = viewModelCon(repositorio)
        assertEquals(AuthError.SessionFailed, viewModel.state.value.error)

        completarYIngresar(viewModel)

        assertEquals(usuario, viewModel.state.value.user)
        assertNull(viewModel.state.value.error)
        assertEquals(2, repositorio.observaciones)
    }

    @Test
    fun siLaObservacionVuelveAFallarElLoginExitosoIgualMuestraAlUsuario() = runTest {
        val repositorio = RepositorioFalso(
            resultadoAcceso = ResultadoAcceso.Exito(usuario),
            fallasDelFlow = 2,
        )
        val viewModel = viewModelCon(repositorio)

        completarYIngresar(viewModel)

        assertEquals(usuario, viewModel.state.value.user)
    }

    @Test
    fun trasFallaDelFlowUnLoginFallidoMuestraElErrorDelLogin() = runTest {
        val viewModel = viewModelCon(
            RepositorioFalso(
                resultadoAcceso = ResultadoAcceso.Fallo(ErrorAuth.CredencialesInvalidas),
                fallasDelFlow = 1,
            ),
        )

        completarYIngresar(viewModel)

        assertNull(viewModel.state.value.user)
        assertEquals(AuthError.InvalidCredentials, viewModel.state.value.error)
    }

    private fun loginFallido(error: ErrorAuth, esperado: AuthError) = runTest {
        val viewModel = viewModelCon(RepositorioFalso(resultadoAcceso = ResultadoAcceso.Fallo(error)))

        completarYIngresar(viewModel)

        assertEquals(esperado, viewModel.state.value.error)
        assertNull(viewModel.state.value.user)
        assertFalse(viewModel.state.value.isLoading)
    }

    private fun completarYIngresar(viewModel: AuthViewModel) {
        viewModel.onEmailChanged("persona@ejemplo.com")
        viewModel.onPasswordChanged("secreto")
        viewModel.signIn()
    }

    private fun viewModelCon(repositorio: RepositorioFalso) = AuthViewModel(
        login = IniciarSesion(repositorio),
        observarSesion = ObservarSesion(repositorio),
        cerrarSesion = CerrarSesion(repositorio),
    )
}

/** Imita a Firebase: iniciar o cerrar sesión con éxito actualiza el Flow de sesión. */
private class RepositorioFalso(
    sesion: Usuario? = null,
    private val resultadoAcceso: ResultadoAcceso? = null,
    private val resultadoCerrarSesion: ResultadoCerrarSesion = ResultadoCerrarSesion.Exito,
    private var fallasDelFlow: Int = 0,
) : RepositorioAutenticacion {
    private val sesionActual = MutableStateFlow(sesion)
    var loginCalls = 0
    var observaciones = 0

    override val usuarioActual: Flow<Usuario?>
        get() = flow {
            observaciones++
            if (fallasDelFlow > 0) {
                fallasDelFlow--
                throw IllegalStateException("falla simulada del Flow de sesión")
            }
            emitAll(sesionActual)
        }

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso {
        loginCalls++
        val resultado = resultadoAcceso ?: ResultadoAcceso.Exito(Usuario("u1", email, null))
        if (resultado is ResultadoAcceso.Exito) sesionActual.value = resultado.usuario
        return resultado
    }

    override suspend fun cerrarSesion(): ResultadoCerrarSesion {
        if (resultadoCerrarSesion is ResultadoCerrarSesion.Exito) sesionActual.value = null
        return resultadoCerrarSesion
    }
}
