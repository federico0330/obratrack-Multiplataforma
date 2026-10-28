package com.unaj.obratrack.core.data.auth

import com.unaj.obratrack.core.firebase.auth.FirebaseAuthDataSource
import com.unaj.obratrack.domain.model.Usuario
import com.unaj.obratrack.domain.repository.RepositorioAutenticacion
import com.unaj.obratrack.domain.repository.ResultadoAcceso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

/** Adapta los DTO de infraestructura a los modelos que consume la aplicación. */
class RepositorioAutenticacionImpl(
    private val remote: FirebaseAuthDataSource,
) : RepositorioAutenticacion {
    override val usuarioActual = remote.currentUser.map { it?.toDomain() }

    override suspend fun iniciarSesion(email: String, clave: String): ResultadoAcceso = try {
        ResultadoAcceso.Exito(remote.signIn(email, clave).toDomain())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        // Firebase-specific error text stays on the data boundary and is never shown to users.
        val detail = error.message.orEmpty()
        when {
            detail.contains("network", ignoreCase = true) -> ResultadoAcceso.SinConexion
            detail.contains("credential", ignoreCase = true) ||
                detail.contains("password", ignoreCase = true) ||
                detail.contains("user-not-found", ignoreCase = true) -> ResultadoAcceso.CredencialesInvalidas
            else -> ResultadoAcceso.ErrorInesperado
        }
    }

    override suspend fun cerrarSesion() = remote.signOut()

    private fun com.unaj.obratrack.core.firebase.auth.AuthUserDto.toDomain() =
        Usuario(id = id, email = email, nombre = displayName)
}
