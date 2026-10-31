package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.ErrorRegistro
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import com.unaj.obratrack.domain.repository.ResultadoCerrarSesion
import com.unaj.obratrack.domain.repository.ResultadoRegistro
import com.unaj.obratrack.domain.usecase.RegistrarUsuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun registroExitosoLimpiaElFormularioSinError() = runTest {
        val repositorio = RepositorioRegistroFalso()
        val viewModel = viewModelCon(repositorio)

        completar(viewModel)
        viewModel.submit()

        assertEquals(1, repositorio.registrarCalls)
        assertEquals("persona@ejemplo.com", repositorio.ultimoEmail)
        assertEquals(RegistroUiState(), viewModel.state.value)
    }

    @Test
    fun emailYaEnUsoLlegaAlEstado() = falloDelRepositorio(ErrorRegistro.EmailYaEnUso, RegistroError.EmailInUse)

    @Test
    fun contrasenaDebilDelRepositorioLlegaAlEstado() =
        falloDelRepositorio(ErrorRegistro.ContrasenaDebil, RegistroError.WeakPassword)

    @Test
    fun emailInvalidoDelRepositorioLlegaAlEstado() =
        falloDelRepositorio(ErrorRegistro.EmailInvalido, RegistroError.InvalidEmail)

    @Test
    fun sinConexionLlegaAlEstado() = falloDelRepositorio(ErrorRegistro.SinConexion, RegistroError.NoConnection)

    @Test
    fun demasiadosIntentosLlegaAlEstado() =
        falloDelRepositorio(ErrorRegistro.DemasiadosIntentos, RegistroError.TooManyAttempts)

    @Test
    fun inesperadoLlegaAlEstado() = falloDelRepositorio(ErrorRegistro.Inesperado, RegistroError.Unexpected)

    @Test
    fun emailInvalidoSeRechazaSinLlamarAlRepositorio() = runTest {
        val repositorio = RepositorioRegistroFalso()
        val viewModel = viewModelCon(repositorio)

        completar(viewModel, email = "no-es-un-correo")
        viewModel.submit()

        assertEquals(RegistroError.InvalidEmail, viewModel.state.value.error)
        assertEquals(0, repositorio.registrarCalls)
    }

    @Test
    fun contrasenaCortaSeRechazaSinLlamarAlRepositorio() = runTest {
        val repositorio = RepositorioRegistroFalso()
        val viewModel = viewModelCon(repositorio)

        completar(viewModel, password = "123", repeat = "123")
        viewModel.submit()

        assertEquals(RegistroError.WeakPassword, viewModel.state.value.error)
        assertEquals(0, repositorio.registrarCalls)
    }

    @Test
    fun contrasenasDistintasNoLlamanAlRepositorio() = runTest {
        val repositorio = RepositorioRegistroFalso()
        val viewModel = viewModelCon(repositorio)

        completar(viewModel, password = "secreto1", repeat = "secreto2")
        viewModel.submit()

        assertEquals(RegistroError.PasswordsDoNotMatch, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(0, repositorio.registrarCalls)
    }

    @Test
    fun camposVaciosNoHacenNada() = runTest {
        val repositorio = RepositorioRegistroFalso()
        val viewModel = viewModelCon(repositorio)

        viewModel.submit()
        viewModel.onEmailChanged("persona@ejemplo.com")
        viewModel.submit()

        assertNull(viewModel.state.value.error)
        assertEquals(0, repositorio.registrarCalls)
    }

    @Test
    fun editarUnCampoLimpiaElError() = runTest {
        val viewModel = viewModelCon(RepositorioRegistroFalso(resultado = ResultadoRegistro.Fallo(ErrorRegistro.EmailYaEnUso)))
        completar(viewModel)
        viewModel.submit()

        viewModel.onEmailChanged("otra@ejemplo.com")

        assertNull(viewModel.state.value.error)
    }

    private fun falloDelRepositorio(error: ErrorRegistro, esperado: RegistroError) = runTest {
        val viewModel = viewModelCon(RepositorioRegistroFalso(resultado = ResultadoRegistro.Fallo(error)))

        completar(viewModel)
        viewModel.submit()

        assertEquals(esperado, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    private fun completar(
        viewModel: RegistroViewModel,
        email: String = "persona@ejemplo.com",
        password: String = "secreto1",
        repeat: String = password,
    ) {
        viewModel.onEmailChanged(email)
        viewModel.onPasswordChanged(password)
        viewModel.onRepeatPasswordChanged(repeat)
    }

    private fun viewModelCon(repositorio: RepositorioRegistroFalso) = RegistroViewModel(RegistrarUsuario(repositorio))
}

private class RepositorioRegistroFalso(
    private val resultado: ResultadoRegistro? = null,
) : RepositorioAutenticacion {
    var registrarCalls = 0
    var ultimoEmail: String? = null

    override val usuarioActual: Flow<Usuario?> = MutableStateFlow(null)

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso =
        error("no se usa en el registro")

    override suspend fun cerrarSesion(): ResultadoCerrarSesion = error("no se usa en el registro")

    override suspend fun registrar(email: String, contrasena: String): ResultadoRegistro {
        registrarCalls++
        ultimoEmail = email
        return resultado ?: ResultadoRegistro.Exito(Usuario("u1", email, null))
    }
}
