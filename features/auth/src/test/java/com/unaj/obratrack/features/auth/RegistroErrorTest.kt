package com.unaj.obratrack.features.auth

import com.unaj.obratrack.domain.repository.ErrorRegistro
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistroErrorTest {
    @Test
    fun emailInvalido() = assertEquals(RegistroError.InvalidEmail, ErrorRegistro.EmailInvalido.toRegistroError())

    @Test
    fun contrasenaDebil() = assertEquals(RegistroError.WeakPassword, ErrorRegistro.ContrasenaDebil.toRegistroError())

    @Test
    fun emailYaEnUso() = assertEquals(RegistroError.EmailInUse, ErrorRegistro.EmailYaEnUso.toRegistroError())

    @Test
    fun sinConexion() = assertEquals(RegistroError.NoConnection, ErrorRegistro.SinConexion.toRegistroError())

    @Test
    fun demasiadosIntentos() =
        assertEquals(RegistroError.TooManyAttempts, ErrorRegistro.DemasiadosIntentos.toRegistroError())

    @Test
    fun inesperado() = assertEquals(RegistroError.Unexpected, ErrorRegistro.Inesperado.toRegistroError())

    @Test
    fun puedeEnviarSoloConCamposCompletosYSinCargar() {
        assertEquals(false, RegistroUiState().canSubmit)
        assertEquals(false, RegistroUiState(email = "a@b.com", password = "secreto").canSubmit)
        assertEquals(false, RegistroUiState(email = "a@b.com", repeatPassword = "secreto").canSubmit)
        assertEquals(false, RegistroUiState(email = " ", password = "secreto", repeatPassword = "secreto").canSubmit)
        assertEquals(true, RegistroUiState(email = "a@b.com", password = "secreto", repeatPassword = "secreto").canSubmit)
        assertEquals(
            false,
            RegistroUiState(email = "a@b.com", password = "secreto", repeatPassword = "secreto", isLoading = true).canSubmit,
        )
    }
}
