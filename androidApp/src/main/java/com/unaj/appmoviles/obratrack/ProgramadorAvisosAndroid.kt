package com.unaj.appmoviles.obratrack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.unaj.obratrack.domain.model.AvisoVisita
import com.unaj.obratrack.domain.repository.ProgramadorAvisos
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Avisos locales de visitas con WorkManager: sobreviven al cierre de la app y al reinicio, y no
 * exigen alarmas exactas (el sistema puede atrasarlos unos minutos para ahorrar batería). Cada aviso
 * es un trabajo único por obra; [AvisoVisitaWorker] publica la notificación cuando llega la hora.
 * Los datos de la obra viajan en las etiquetas del trabajo: así [programados] consulta al propio
 * WorkManager, sin una segunda fuente de verdad.
 */
class ProgramadorAvisosAndroid(private val contexto: Context) : ProgramadorAvisos {
    private val workManager get() = WorkManager.getInstance(contexto)

    override suspend fun programados(): Map<String, Instant> {
        val pendientes = workManager.getWorkInfosByTagFlow(ETIQUETA_AVISO).first()
        return pendientes.filter { it.state == WorkInfo.State.ENQUEUED }.mapNotNull { info ->
            val obraId = info.tags.firstOrNull { it.startsWith(ETIQUETA_OBRA) }?.removePrefix(ETIQUETA_OBRA)
                ?: return@mapNotNull null
            val disparo = info.tags.firstOrNull { it.startsWith(ETIQUETA_DISPARO) }
                ?.removePrefix(ETIQUETA_DISPARO)?.toLongOrNull() ?: return@mapNotNull null
            obraId to Instant.fromEpochMilliseconds(disparo)
        }.toMap()
    }

    override suspend fun programar(aviso: AvisoVisita) {
        crearCanal(contexto)
        val demora = (aviso.disparo - Clock.System.now()).coerceAtLeast(kotlin.time.Duration.ZERO)
        val pedido = OneTimeWorkRequestBuilder<AvisoVisitaWorker>()
            .setInputData(
                workDataOf(
                    CLAVE_OBRA_ID to aviso.obraId,
                    CLAVE_OBRA_NOMBRE to aviso.obraNombre,
                    CLAVE_DIRECCION to aviso.direccion,
                    CLAVE_FECHA to aviso.fecha.toString(),
                    CLAVE_HORA to aviso.horaMinutos,
                ),
            )
            .setInitialDelay(demora.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .addTag(ETIQUETA_AVISO)
            .addTag(ETIQUETA_OBRA + aviso.obraId)
            .addTag(ETIQUETA_DISPARO + aviso.disparo.toEpochMilliseconds())
            .build()
        workManager.enqueueUniqueWork("aviso_" + aviso.obraId, ExistingWorkPolicy.REPLACE, pedido)
    }

    override suspend fun cancelar(obraId: String) {
        workManager.cancelUniqueWork("aviso_$obraId")
    }

    companion object {
        internal const val ETIQUETA_AVISO = "aviso_visita"
        internal const val ETIQUETA_OBRA = "obra:"
        internal const val ETIQUETA_DISPARO = "disparo:"
        internal const val CLAVE_OBRA_ID = "obraId"
        internal const val CLAVE_OBRA_NOMBRE = "obraNombre"
        internal const val CLAVE_DIRECCION = "direccion"
        internal const val CLAVE_FECHA = "fecha"
        internal const val CLAVE_HORA = "hora"
        internal const val CANAL_ID = "avisos_visitas"
    }
}

internal fun crearCanal(contexto: Context) {
    val canal = NotificationChannel(
        ProgramadorAvisosAndroid.CANAL_ID,
        contexto.getString(R.string.aviso_canal),
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    canal.description = contexto.getString(R.string.aviso_canal_descripcion)
    contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
}

class AvisoVisitaWorker(private val contexto: Context, parametros: WorkerParameters) :
    CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val obraId = inputData.getString(ProgramadorAvisosAndroid.CLAVE_OBRA_ID) ?: return Result.failure()
        val obraNombre = inputData.getString(ProgramadorAvisosAndroid.CLAVE_OBRA_NOMBRE) ?: return Result.failure()
        val direccion = inputData.getString(ProgramadorAvisosAndroid.CLAVE_DIRECCION)
        val fechaIso = inputData.getString(ProgramadorAvisosAndroid.CLAVE_FECHA) ?: return Result.failure()
        val horaMinutos = inputData.getInt(ProgramadorAvisosAndroid.CLAVE_HORA, -1)
        if (horaMinutos !in 0 until 24 * 60) return Result.failure()

        val manager = contexto.getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 33 && !manager.areNotificationsEnabled()) {
            // Sin el permiso el aviso no se puede mostrar; la Agenda de visitas sigue siendo la referencia.
            return Result.success()
        }
        crearCanal(contexto)

        val fecha = java.time.LocalDate.parse(fechaIso)
            .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
        val hora = "%02d:%02d".format(horaMinutos / 60, horaMinutos % 60)
        val texto = buildString {
            append(contexto.getString(R.string.aviso_visita, fecha, hora))
            direccion?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        }
        val notificacion = NotificationCompat.Builder(contexto, ProgramadorAvisosAndroid.CANAL_ID)
            .setSmallIcon(R.drawable.ic_avisos_notificacion)
            .setContentTitle(obraNombre)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    contexto,
                    obraId.hashCode(),
                    Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
        manager.notify(obraId.hashCode(), notificacion)
        return Result.success()
    }
}
