package com.unaj.appmoviles.obratrack

import android.content.Intent
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.fromMilliseconds
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.regex.Pattern
import java.io.File

/** Recorre las pantallas reales y limpia únicamente la obra creada para esta ejecución. */
class RelevamientosUiTest {
    @Test
    fun crearDesdePantallaConYSinTareaYPersistirAlReabrir() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("firebaseIntegration") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val uid = requireNotNull(Firebase.auth.currentUser?.uid) { "Abrí una sesión antes de ejecutar la prueba." }
        val obra = Firebase.firestore.collection("obras").document
        val nombre = "PRUEBA_QA_UI_RELEVAMIENTOS"
        val tarea = obra.collection("tareas").document("tarea-ui")
        val relevamientos = obra.collection("relevamientos")
        val abrirApp = {
            device.wakeUp()
            device.executeShellCommand("wm dismiss-keyguard")
            val context = instrumentation.targetContext
            context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            assertTrue("La lista de obras debe estar visible y el teléfono desbloqueado.",
                device.wait(Until.hasObject(By.text(Pattern.compile("My works|Mis obras|I miei lavori"))), 15_000))
        }
        fun encontrar(selector: BySelector): UiObject2 {
            device.wait(Until.findObject(selector), 1_000)?.let { return it }
            for (direccion in listOf(Direction.DOWN, Direction.UP)) {
                for (intento in 0 until 12) {
                    val scroll = device.findObject(By.scrollable(true))
                        ?: error("No hay un contenedor para desplazar la pantalla.")
                    val puedeSeguir = scroll.scroll(direccion, 0.7f)
                    device.wait(Until.findObject(selector), 500)?.let { return it }
                    if (!puedeSeguir) break
                }
            }
            error("No se encontró el control: $selector")
        }
        fun crearNota(nota: String, vincular: Boolean) {
            encontrar(By.res("relevamientos_create")).click()
            val campo = encontrar(By.res("relevamientos_note"))
            val editor = if (campo.className == "android.widget.EditText") campo
                else campo.findObject(By.clazz("android.widget.EditText")) ?: error("No se encontró el editor de nota.")
            editor.click()
            device.waitForIdle()
            device.executeShellCommand("input text $nota")
            assertTrue(device.wait(Until.hasObject(By.res("relevamientos_note").text(nota)), 5_000))
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                val teclado = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                teclado.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            }
            device.waitForIdle()
            if (vincular) {
                encontrar(By.res("relevamientos_task")).click()
                requireNotNull(device.wait(Until.findObject(By.text("TAREA_QA_RELEVAMIENTOS")), 5_000)) {
                    "No se encontró la tarea en el menú."
                }.click()
            }
            encontrar(By.res("relevamientos_save")).click()
            encontrar(By.res("relevamientos_create"))
            assertTrue(encontrar(By.textContains(nota)).text.contains(nota))
        }
        withTimeout(120_000) {
            try {
                obra.set(mapOf(
                    "userId" to uid, "nombre" to nombre, "cliente" to "QA", "direccion" to "Prueba de interfaz",
                    "publica" to false, "estado" to "activa", "fechaInicio" to Timestamp.fromMilliseconds(10.0),
                ))
                tarea.set(mapOf("userId" to uid, "titulo" to "TAREA_QA_RELEVAMIENTOS", "estado" to "PENDIENTE"))
                abrirApp()
                encontrar(By.text(nombre)).click()
                encontrar(By.res("obras_tab_relevamientos")).click()
                crearNota("PRUEBA_QA_SIN_TAREA", false)
                crearNota("PRUEBA_QA_CON_TAREA", true)
                val docs = relevamientos.get(Source.SERVER).documents
                assertEquals(2, docs.size)
                assertTrue(docs.any { it.get<String>("nota") == "PRUEBA_QA_SIN_TAREA" && !it.contains("tareaId") })
                assertTrue(docs.any { it.get<String>("nota") == "PRUEBA_QA_CON_TAREA" && it.get<String>("tareaId") == tarea.id })
                abrirApp()
                encontrar(By.text(nombre)).click()
                encontrar(By.res("obras_tab_relevamientos")).click()
                for (doc in docs) {
                    encontrar(By.res("relevamientos_item_${doc.id}"))
                }
            } catch (error: Throwable) {
                val cache = instrumentation.targetContext.cacheDir
                device.takeScreenshot(File(cache, "relevamientos-ui-fallo.png"))
                device.dumpWindowHierarchy(File(cache, "relevamientos-ui-fallo.xml"))
                throw error
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
