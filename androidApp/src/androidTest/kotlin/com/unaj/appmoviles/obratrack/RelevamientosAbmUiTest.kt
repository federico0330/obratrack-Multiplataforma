package com.unaj.appmoviles.obratrack

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import dev.gitlive.firebase.firestore.toMilliseconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class RelevamientosAbmUiTest {
    @Test
    fun detalleEdicionFiltrosCancelacionYBajaHastaLaListaVacia() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val uid = requireNotNull(Firebase.auth.currentUser?.uid)
        val obra = Firebase.firestore.collection("obras").document
        val registros = obra.collection("relevamientos")
        val tarea = obra.collection("tareas").document("tarea-abm")
        val nombre = "PRUEBA_QA_ABM_${obra.id.take(6)}"
        fun buscar(selector: BySelector): UiObject2 {
            device.waitForIdle()
            device.wait(Until.findObject(selector), 1_000)?.let { return it }
            for (direccion in listOf(Direction.DOWN, Direction.UP)) {
                for (intento in 0 until 12) {
                    val scroll = device.findObject(By.scrollable(true)) ?: error("No hay un listado para desplazar.")
                    val sigue = scroll.scroll(direccion, 0.7f)
                    device.wait(Until.findObject(selector), 500)?.let { return it }
                    if (!sigue) break
                }
            }
            error("No se encontró: $selector")
        }
        fun tocar(selector: BySelector) {
            for (intento in 0 until 3) {
                try {
                    buscar(selector).click()
                    device.waitForIdle()
                    return
                } catch (error: StaleObjectException) {
                    if (intento == 2) throw error
                }
            }
        }
        fun pulsar(id: String) = tocar(By.res(id))
        fun boton(id: String) = buscar(By.res(id))
        fun opcion(texto: String) = requireNotNull(device.wait(Until.findObject(By.text(Pattern.compile(texto))), 5_000))
        fun confirmarBaja(id: String) {
            pulsar("relevamientos_item_$id")
            pulsar("relevamientos_delete")
            opcion("Confirm deletion|Confirmar eliminación|Conferma eliminazione").click()
            buscar(By.res("relevamientos_create").enabled(true))
        }
        withTimeout(180_000) {
            try {
                obra.set(mapOf("userId" to uid, "nombre" to nombre, "cliente" to "QA", "direccion" to "Prueba ABM",
                    "publica" to false, "estado" to "activa", "fechaInicio" to Timestamp.fromMilliseconds(10.0),
                    "ultimaVisita" to Timestamp.fromMilliseconds(1793577600000.0)))
                tarea.set(mapOf("userId" to uid, "titulo" to "TAREA_QA_ABM", "estado" to "PENDIENTE"))
                for ((id, fecha) in listOf("editar" to 1793577600000.0, "otro" to 1793491200000.0)) {
                    registros.document(id).set(mapOf("userId" to uid, "fecha" to Timestamp.fromMilliseconds(fecha),
                        "nota" to "PRUEBA_QA_$id", "tareaId" to tarea.id))
                }
                device.wakeUp()
                device.executeShellCommand("wm dismiss-keyguard")
                val context = instrumentation.targetContext
                context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                assertTrue(device.wait(Until.hasObject(By.text(Pattern.compile("My works|Mis obras|I miei lavori"))), 15_000))
                tocar(By.text(nombre))
                pulsar("relevamientos_item_editar")
                pulsar("relevamientos_edit")
                // Atrás cierra el formulario y mantiene el detalle del relevamiento abierto.
                device.pressBack()
                pulsar("relevamientos_edit")
                val editor = boton("relevamientos_note")
                editor.click()
                device.waitForIdle()
                device.pressKeyCode(KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON)
                device.pressKeyCode(KeyEvent.KEYCODE_DEL)
                device.executeShellCommand("input text PRUEBA_QA_EDITADA")
                assertTrue(device.wait(Until.hasObject(By.res("relevamientos_note").text("PRUEBA_QA_EDITADA")), 5_000))
                instrumentation.runOnMainSync {
                    val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
                }
                device.waitForIdle()
                pulsar("relevamientos_task")
                opcion("No linked task|Sin tarea vinculada|Nessuna attività collegata").click()
                pulsar("relevamientos_save")
                boton("relevamientos_edit")
                assertEquals("PRUEBA_QA_EDITADA", registros.document("editar").get(Source.SERVER).get<String>("nota"))
                assertFalse(registros.document("editar").get(Source.SERVER).contains("tareaId"))
                pulsar("relevamientos_back")
                pulsar("relevamientos_filter")
                opcion("No linked task|Sin tarea vinculada|Nessuna attività collegata").click()
                boton("relevamientos_item_editar")
                assertFalse(device.hasObject(By.res("relevamientos_item_otro")))
                pulsar("relevamientos_filter")
                opcion("TAREA_QA_ABM").click()
                boton("relevamientos_item_otro")
                assertFalse(device.hasObject(By.res("relevamientos_item_editar")))
                pulsar("relevamientos_filter")
                opcion("All surveys|Todos los relevamientos|Tutti i rilievi").click()
                pulsar("relevamientos_item_editar")
                pulsar("relevamientos_delete")
                opcion("Cancel|Cancelar|Annulla").click()
                assertTrue(registros.document("editar").get(Source.SERVER).exists)
                pulsar("relevamientos_back")
                confirmarBaja("editar")
                assertEquals(1793491200000L, obra.get(Source.SERVER).get<Timestamp>("ultimaVisita").toMilliseconds().toLong())
                confirmarBaja("otro")
                assertTrue(registros.get(Source.SERVER).documents.isEmpty())
                assertFalse(obra.get(Source.SERVER).contains("ultimaVisita"))
            } catch (error: Throwable) {
                device.takeScreenshot(File(instrumentation.targetContext.cacheDir, "relevamientos-abm-fallo.png"))
                device.dumpWindowHierarchy(File(instrumentation.targetContext.cacheDir, "relevamientos-abm-fallo.xml"))
                throw error
            } finally {
                withContext(NonCancellable) {
                    withTimeout(30_000) {
                        for (doc in registros.get(Source.SERVER).documents) doc.reference.delete()
                        tarea.delete()
                        obra.delete()
                    }
                }
            }
        }
    }
}
