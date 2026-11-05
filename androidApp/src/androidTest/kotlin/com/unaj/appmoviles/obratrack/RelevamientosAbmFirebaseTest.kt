package com.unaj.appmoviles.obratrack

import androidx.test.platform.app.InstrumentationRegistry
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoFailure
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoResult
import com.unaj.obratrack.core.firebase.relevamientos.GitLiveFirebaseRelevamientoDataSource
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoUpdateFields
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class RelevamientosAbmFirebaseTest {
    @Test
    fun editarYBorrarConservaDatosNativosYRecalculaUltimaVisita() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val uid = requireNotNull(Firebase.auth.currentUser?.uid)
        withTimeout(120_000) {
            val db = Firebase.firestore
            val obra = db.collection("obras").document
            val relevamientos = obra.collection("relevamientos")
            val tarea = obra.collection("tareas").document("tarea-qa")
            val adapter = GitLiveFirebaseRelevamientoDataSource(db)
            fun fecha(ms: Long) = Timestamp.fromMilliseconds(ms.toDouble())
            try {
                obra.set(mapOf("userId" to uid, "nombre" to "PRUEBA_QA_ABM_RELEVAMIENTOS", "cliente" to "QA",
                    "direccion" to "Prueba de integración", "publica" to false, "estado" to "activa",
                    "fechaInicio" to fecha(10), "ultimaVisita" to fecha(30), "proximaVisita" to fecha(70), "horaVisita" to 600))
                tarea.set(mapOf("userId" to uid, "titulo" to "Tarea QA", "estado" to "PENDIENTE"))
                relevamientos.document("anterior").set(mapOf("userId" to uid, "fecha" to fecha(10), "nota" to "Anterior"))
                val ultimo = relevamientos.document("ultimo")
                ultimo.set(mapOf("userId" to uid, "fecha" to fecha(30), "nota" to "Original", "tareaId" to tarea.id,
                    "fotoUrl" to "https://example.com/qa.jpg", "ubicacion" to GeoPoint(-34.0, -58.0),
                    "proximaVisita" to fecha(70), "horaVisita" to 600, "campoNativoExtra" to "conservar"))
                assertTrue(adapter.actualizar(uid, obra.id, "ultimo", RelevamientoUpdateFields(5, "Editado", null)) is FirebaseRelevamientoResult.Success)
                val editado = ultimo.get(Source.SERVER)
                assertEquals("Editado", editado.get<String>("nota"))
                assertFalse(editado.contains("tareaId"))
                assertEquals(uid, editado.get<String>("userId"))
                assertEquals("https://example.com/qa.jpg", editado.get<String>("fotoUrl"))
                assertEquals(-34.0, editado.get<GeoPoint>("ubicacion").latitude, 0.0)
                assertEquals(600, editado.get<Int>("horaVisita"))
                assertEquals(70L, editado.get<Timestamp>("proximaVisita").toMilliseconds().toLong())
                assertEquals("conservar", editado.get<String>("campoNativoExtra"))
                assertEquals(10L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                assertTrue(adapter.actualizar(uid, obra.id, "anterior", RelevamientoUpdateFields(40, "Otro", tarea.id)) is FirebaseRelevamientoResult.Success)
                assertEquals(tarea.id, relevamientos.document("anterior").get(Source.SERVER).get<String>("tareaId"))
                assertEquals(40L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                assertTrue(adapter.eliminar(uid, obra.id, "anterior") is FirebaseRelevamientoResult.Success)
                assertEquals(5L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                assertTrue(adapter.eliminar(uid, obra.id, "ultimo") is FirebaseRelevamientoResult.Success)
                assertFalse(obra.get(Source.SERVER).contains("ultimaVisita"))
                assertEquals(600, obra.get(Source.SERVER).get<Int>("horaVisita"))
                assertEquals(70L, obra.get(Source.SERVER).get<Timestamp>("proximaVisita").toMilliseconds().toLong())
                assertTrue(tarea.get(Source.SERVER).exists)
                assertEquals(FirebaseRelevamientoResult.Failure(FirebaseRelevamientoFailure.NotFound), adapter.eliminar(uid, obra.id, "ultimo"))

                // Crear y mover el más reciente a la vez debe dejar el máximo real del conjunto.
                for (vuelta in 1..3) {
                    relevamientos.document("mover").set(mapOf("userId" to uid, "fecha" to fecha(30), "nota" to "Mover"))
                    obra.updateFields { "ultimaVisita" to fecha(30) }
                    coroutineScope {
                        val editar = async { adapter.actualizar(uid, obra.id, "mover", RelevamientoUpdateFields(5, "Movido", null)) }
                        val crear = async { adapter.crear(RelevamientoDto("", obra.id, uid, 20, "Concurrente")) }
                        assertTrue(editar.await() is FirebaseRelevamientoResult.Success)
                        assertTrue(crear.await() is FirebaseRelevamientoResult.Success)
                    }
                    assertEquals(20L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                    for (doc in relevamientos.get(Source.SERVER).documents) doc.reference.delete()
                }
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
