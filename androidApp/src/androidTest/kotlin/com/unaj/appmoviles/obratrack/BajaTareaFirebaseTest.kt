package com.unaj.appmoviles.obratrack

import androidx.test.platform.app.InstrumentationRegistry
import com.unaj.obratrack.core.firebase.relevamientos.FirebaseRelevamientoResult
import com.unaj.obratrack.core.firebase.relevamientos.GitLiveFirebaseRelevamientoDataSource
import com.unaj.obratrack.core.firebase.relevamientos.RelevamientoDto
import com.unaj.obratrack.core.firebase.tareas.FirebaseTareaResult
import com.unaj.obratrack.core.firebase.tareas.GitLiveFirebaseTareaDataSource
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Requiere activar firebaseIntegration y tener una sesión abierta en la app del dispositivo. */
class BajaTareaFirebaseTest {
    @Test
    fun bajaConcurrenteConAltaConservaLaUltimaVisitaDelConjunto() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val uid = requireNotNull(Firebase.auth.currentUser?.uid)
        val db = Firebase.firestore
        val tareas = GitLiveFirebaseTareaDataSource(db)
        val relevamientos = GitLiveFirebaseRelevamientoDataSource(db)
        withTimeout(120_000) {
            repeat(5) {
                val obra = db.collection("obras").document
                val tarea = obra.collection("tareas").document("retirar")
                val registros = obra.collection("relevamientos")
                try {
                    obra.set(mapOf(
                        "userId" to uid, "nombre" to "Prueba de concurrencia", "cliente" to "Integración",
                        "direccion" to "Prueba local", "publica" to false, "estado" to "activa",
                        "fechaInicio" to Timestamp.fromMilliseconds(1.0),
                        "ultimaVisita" to Timestamp.fromMilliseconds(30.0),
                    ))
                    tarea.set(mapOf("userId" to uid, "titulo" to "Retirar", "estado" to "PENDIENTE"))
                    registros.document("anterior").set(mapOf(
                        "userId" to uid, "fecha" to Timestamp.fromMilliseconds(10.0), "nota" to "Anterior",
                    ))
                    registros.document("vinculado").set(mapOf(
                        "userId" to uid, "fecha" to Timestamp.fromMilliseconds(30.0),
                        "nota" to "Vinculado", "tareaId" to tarea.id,
                    ))
                    coroutineScope {
                        val baja = async { tareas.eliminar(uid, obra.id, tarea.id, true) }
                        val alta = async { relevamientos.crear(RelevamientoDto("", obra.id, uid, 20, "Nuevo")) }
                        assertTrue(baja.await() is FirebaseTareaResult.Success)
                        assertTrue(alta.await() is FirebaseRelevamientoResult.Success)
                    }
                    assertEquals(20L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                    assertEquals(2, registros.get(Source.SERVER).documents.size)
                    assertFalse(tarea.get(Source.SERVER).exists)
                } finally {
                    withContext(NonCancellable) {
                        withTimeout(20_000) {
                            for (doc in registros.get(Source.SERVER).documents) doc.reference.delete()
                            tarea.delete()
                            obra.delete()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun bajaYDesvinculacionConservanLaUltimaVisitaEnServidor() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val uid = requireNotNull(Firebase.auth.currentUser?.uid) { "Abrí una sesión antes de ejecutar la prueba." }
        withTimeout(90_000) {
            val db = Firebase.firestore
            val obra = db.collection("obras").document
            val tareas = obra.collection("tareas")
            val relevamientos = obra.collection("relevamientos")
            fun fecha(millis: Long) = Timestamp.fromMilliseconds(millis.toDouble())
            try {
                obra.set(mapOf(
                    "userId" to uid, "nombre" to "PRUEBA_QA_BAJA_TAREAS", "cliente" to "QA",
                    "direccion" to "Prueba de integración", "publica" to false,
                    "estado" to "activa", "fechaInicio" to fecha(10L), "ultimaVisita" to fecha(30L),
                ))
                for (id in listOf("anterior", "reciente")) {
                    tareas.document(id).set(mapOf("userId" to uid, "titulo" to id, "estado" to "PENDIENTE"))
                }
                relevamientos.document("viejo").set(mapOf("userId" to uid, "tareaId" to "anterior", "fecha" to fecha(10L)))
                relevamientos.document("nuevo").set(mapOf("userId" to uid, "tareaId" to "reciente", "fecha" to fecha(30L)))
                val adapter = GitLiveFirebaseTareaDataSource(db)
                assertTrue(adapter.eliminar(uid, obra.id, "reciente", true) is FirebaseTareaResult.Success)
                assertFalse(tareas.document("reciente").get(Source.SERVER).exists)
                assertFalse(relevamientos.document("nuevo").get(Source.SERVER).exists)
                assertEquals(10L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())

                assertTrue(adapter.eliminar(uid, obra.id, "anterior", false) is FirebaseTareaResult.Success)
                val conservado = relevamientos.document("viejo").get(Source.SERVER)
                assertTrue(conservado.exists)
                assertFalse(conservado.contains("tareaId"))
                assertEquals(10L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())

                tareas.document("ultima").set(mapOf("userId" to uid, "titulo" to "Última", "estado" to "PENDIENTE"))
                relevamientos.document("viejo").update("tareaId" to "ultima")
                assertTrue(adapter.eliminar(uid, obra.id, "ultima", true) is FirebaseTareaResult.Success)
                assertFalse(relevamientos.document("viejo").get(Source.SERVER).exists)
                assertFalse(obra.get(Source.SERVER).contains("ultimaVisita"))
            } finally {
                // Solo se limpia la obra creada por esta ejecución y sus documentos conocidos.
                withContext(NonCancellable) {
                    withTimeout(30_000) {
                        for (id in listOf("viejo", "nuevo")) relevamientos.document(id).delete()
                        for (id in listOf("anterior", "reciente", "ultima")) tareas.document(id).delete()
                        obra.delete()
                    }
                }
            }
        }
    }
}
