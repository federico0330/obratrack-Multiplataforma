package com.unaj.obratrack.features.auth

import androidx.lifecycle.viewModelScope
import com.unaj.obratrack.domain.repository.ErrorActualizarNombre
import com.unaj.obratrack.domain.repository.ErrorRecuperarContrasena
import com.unaj.obratrack.domain.repository.RepositorioCuenta
import com.unaj.obratrack.domain.repository.ResultadoActualizarNombre
import com.unaj.obratrack.domain.repository.ResultadoRecuperarContrasena
import com.unaj.obratrack.domain.usecase.ActualizarNombreUsuario
import com.unaj.obratrack.domain.usecase.RecuperarContrasena
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CuentaViewModelTest {
    private fun conViewModel(
        cuenta: CuentaFalsa = CuentaFalsa(),
        bloque: suspend TestScope.(CuentaViewModel) -> Unit,
    ) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val viewModel = CuentaViewModel(RecuperarContrasena(cuenta), ActualizarNombreUsuario(cuenta))
        try {
            bloque(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun abrirRecuperacionPrecargaElCorreoRecortado() = conViewModel { viewModel ->
        viewModel.abrirRecuperacion("  ana@mail.com ")

        assertEquals("ana@mail.com", viewModel.state.value.recuperacion?.email)
    }

    @Test
    fun enviarRecuperacionMarcaEnviado() {
        val cuenta = CuentaFalsa()
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirRecuperacion("ana@mail.com")

            viewModel.enviarRecuperacion()

            val formulario = viewModel.state.value.recuperacion
            assertTrue(formulario?.enviado == true)
            assertEquals(false, formulario?.enviando)
            assertEquals("ana@mail.com", cuenta.ultimoCorreo)
        }
    }

    @Test
    fun correoInvalidoMuestraErrorSinEnviar() {
        val cuenta = CuentaFalsa()
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirRecuperacion("ana@")

            viewModel.enviarRecuperacion()

            assertEquals(ErrorRecuperacionUi.EmailInvalido, viewModel.state.value.recuperacion?.error)
            assertEquals(0, cuenta.recuperarCalls)
        }
    }

    @Test
    fun sinConexionConservaElCorreoYMuestraElError() {
        val cuenta = CuentaFalsa(
            recuperar = ResultadoRecuperarContrasena.Fallo(ErrorRecuperarContrasena.SinConexion),
        )
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirRecuperacion("ana@mail.com")

            viewModel.enviarRecuperacion()

            val formulario = viewModel.state.value.recuperacion
            assertEquals(ErrorRecuperacionUi.SinConexion, formulario?.error)
            assertEquals("ana@mail.com", formulario?.email)
            assertEquals(false, formulario?.enviado)
        }
    }

    @Test
    fun editarElCorreoLimpiaElError() = conViewModel { viewModel ->
        viewModel.abrirRecuperacion("ana@")
        viewModel.enviarRecuperacion()

        viewModel.emailRecuperacionCambiado("ana@mail.com")

        assertNull(viewModel.state.value.recuperacion?.error)
    }

    @Test
    fun noSeEnviaDosVecesMientrasEspera() {
        val cuenta = CuentaFalsa(compuertaRecuperar = CompletableDeferred())
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirRecuperacion("ana@mail.com")

            viewModel.enviarRecuperacion()
            viewModel.enviarRecuperacion()
            cuenta.compuertaRecuperar!!.complete(Unit)

            assertEquals(1, cuenta.recuperarCalls)
        }
    }

    @Test
    fun cerrarMientrasEsperaNoReabreElDialogo() {
        val cuenta = CuentaFalsa(compuertaRecuperar = CompletableDeferred())
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirRecuperacion("ana@mail.com")
            viewModel.enviarRecuperacion()

            viewModel.cerrarRecuperacion()
            cuenta.compuertaRecuperar!!.complete(Unit)

            assertNull(viewModel.state.value.recuperacion)
        }
    }

    @Test
    fun guardarNombreCierraElDialogoYEnviaElNombreRecortado() {
        val cuenta = CuentaFalsa()
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirEdicionNombre("Ana")
            viewModel.nombreCambiado("  Ana Pérez ")

            viewModel.guardarNombre()

            assertNull(viewModel.state.value.edicionNombre)
            assertEquals("Ana Pérez", cuenta.ultimoNombre)
        }
    }

    @Test
    fun nombreEnBlancoNoSeGuarda() {
        val cuenta = CuentaFalsa()
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirEdicionNombre("Ana")
            viewModel.nombreCambiado("   ")

            viewModel.guardarNombre()

            assertEquals(0, cuenta.nombreCalls)
            assertEquals(false, viewModel.state.value.edicionNombre?.puedeGuardar)
        }
    }

    @Test
    fun falloAlGuardarConservaElTextoYMuestraElError() {
        val cuenta = CuentaFalsa(
            nombre = ResultadoActualizarNombre.Fallo(ErrorActualizarNombre.SinConexion),
        )
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirEdicionNombre("Ana")
            viewModel.nombreCambiado("Ana Pérez")

            viewModel.guardarNombre()

            val formulario = viewModel.state.value.edicionNombre
            assertEquals(ErrorNombreUi.SinConexion, formulario?.error)
            assertEquals("Ana Pérez", formulario?.nombre)
            assertEquals(false, formulario?.guardando)
        }
    }

    @Test
    fun cerrarMientrasGuardaNoReabreElDialogo() {
        val cuenta = CuentaFalsa(compuertaNombre = CompletableDeferred())
        conViewModel(cuenta) { viewModel ->
            viewModel.abrirEdicionNombre("Ana")
            viewModel.nombreCambiado("Ana Pérez")
            viewModel.guardarNombre()

            viewModel.cerrarEdicionNombre()
            cuenta.compuertaNombre!!.complete(Unit)

            assertNull(viewModel.state.value.edicionNombre)
        }
    }
}

private class CuentaFalsa(
    private val recuperar: ResultadoRecuperarContrasena = ResultadoRecuperarContrasena.Exito,
    private val nombre: ResultadoActualizarNombre = ResultadoActualizarNombre.Exito,
    val compuertaRecuperar: CompletableDeferred<Unit>? = null,
    val compuertaNombre: CompletableDeferred<Unit>? = null,
) : RepositorioCuenta {
    var recuperarCalls = 0
    var ultimoCorreo: String? = null
    var nombreCalls = 0
    var ultimoNombre: String? = null

    override suspend fun recuperarContrasena(email: String): ResultadoRecuperarContrasena {
        recuperarCalls++
        ultimoCorreo = email
        compuertaRecuperar?.await()
        return recuperar
    }

    override suspend fun actualizarNombre(nombre: String): ResultadoActualizarNombre {
        nombreCalls++
        ultimoNombre = nombre
        compuertaNombre?.await()
        return this.nombre
    }
}
