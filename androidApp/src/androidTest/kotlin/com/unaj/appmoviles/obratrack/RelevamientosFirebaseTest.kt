package com.unaj.appmoviles.obratrack

import androidx.test.platform.app.InstrumentationRegistry
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoFailure
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoResult
import com.unaj.obratrack.core.firebase.relevamientos.GitLiveFirebaseRelevamientoDataSource
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RelevamientosFirebaseTest {
    @Test
    fun altaListadoYVinculoOpcionalRespetanElEsquemaYLaUltimaVisita() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val uid = requireNotNull(Firebase.auth.currentUser?.uid) { "Abrí una sesión antes de ejecutar la prueba." }
        withTimeout(90_000) {
            val db = Firebase.firestore
            val obra = db.collection("obras").document
            val relevamientos = obra.collection("relevamientos")
            val tarea = obra.collection("tareas").document("tarea-qa")
            val adapter = GitLiveFirebaseRelevamientoDataSource(db)
            fun fecha(millis: Long) = Timestamp.fromMilliseconds(millis.toDouble())
            try {
                obra.set(mapOf(
                    "userId" to uid, "nombre" to "PRUEBA_QA_ALTA_RELEVAMIENTOS", "cliente" to "QA",
                    "direccion" to "Prueba de integración", "publica" to false, "estado" to "activa",
                    "fechaInicio" to fecha(10), "ultimaVisita" to fecha(30),
                ))
                tarea.set(mapOf("userId" to uid, "titulo" to "Tarea QA", "estado" to "PENDIENTE"))
                val sinVinculo = adapter.crear(RelevamientoDto("", obra.id, uid, 20, "Nota sin tarea"))
                assertTrue(sinVinculo is FirebaseRelevamientoResult.Success)
                val idSinVinculo = (sinVinculo as FirebaseRelevamientoResult.Success).value.id
                assertFalse(relevamientos.document(idSinVinculo).get(Source.SERVER).contains("tareaId"))
                assertEquals(30L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())

                val conVinculo = adapter.crear(RelevamientoDto("", obra.id, uid, 40, "Nota con tarea", tarea.id))
                assertTrue(conVinculo is FirebaseRelevamientoResult.Success)
                val idConVinculo = (conVinculo as FirebaseRelevamientoResult.Success).value.id
                assertEquals(tarea.id, relevamientos.document(idConVinculo).get(Source.SERVER).get<String>("tareaId"))
                assertEquals(40L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                val lista = adapter.observarDeObra(uid, obra.id).first {
                    it is FirebaseRelevamientoResult.Success && it.value.size == 2
                } as FirebaseRelevamientoResult.Success
                assertEquals(setOf(idSinVinculo, idConVinculo), lista.value.map { it.id }.toSet())

                val fallido = adapter.crear(RelevamientoDto("", obra.id, uid, 60, "No debe guardarse", "inexistente"))
                assertEquals(FirebaseRelevamientoResult.Failure(FirebaseRelevamientoFailure.TaskUnavailable), fallido)
                assertEquals(2, relevamientos.get(Source.SERVER).documents.size)
                assertEquals(40L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
            } finally {
                withContext(NonCancellable) {
                    withTimeout(30_000) {
                        for (doc in relevamientos.get(Source.SERVER).documents) doc.reference.delete()
                        tarea.delete()
                        obra.delete()
                    }
                }
            }
        }
    }
}
