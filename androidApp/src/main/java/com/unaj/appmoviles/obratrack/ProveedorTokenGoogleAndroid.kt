package com.unaj.appmoviles.obratrack

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.unaj.obratrack.domain.repository.ProveedorTokenGoogle
import com.unaj.obratrack.domain.repository.ResultadoTokenGoogle
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pide el idToken con Credential Manager (flujo de botón). Vive en la Application para sobrevivir
 * a la rotación igual que los ViewModel; la Activity se engancha con [adjuntar] y se guarda débil.
 */
class ProveedorTokenGoogleAndroid(private val webClientId: String) : ProveedorTokenGoogle {
    private var actividad: WeakReference<Activity>? = null

    fun adjuntar(activity: Activity) {
        actividad = WeakReference(activity)
    }

    override suspend fun obtenerIdToken(): ResultadoTokenGoogle {
        val activity = actividad?.get() ?: return ResultadoTokenGoogle.Fallo
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                ResultadoTokenGoogle.Exito(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                ResultadoTokenGoogle.Fallo
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: GetCredentialCancellationException) {
            ResultadoTokenGoogle.Cancelado
        } catch (error: GetCredentialException) {
            // Credential Manager no tiene una excepción propia de red: solo se reconoce si la causa es de E/S.
            if (error.cause is IOException) ResultadoTokenGoogle.SinConexion else ResultadoTokenGoogle.Fallo
        } catch (_: GoogleIdTokenParsingException) {
            ResultadoTokenGoogle.Fallo
        }
    }
}
