package com.example.meditrack

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.meditrack.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "meditrack_channel"
    const val CHANNEL_NAME = "Recordatorios de medicamentos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos 5 minutos antes de cada toma"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarms(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("meditrack_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notificaciones_activas", false)) {
            cancelAllAlarms(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val horarios = db.horarioDao().getTodosLosHorarios()
            // We collect once
            horarios.collect { lista ->
                lista.forEach { horario ->
                    val cal = Calendar.getInstance().apply {
                        val h = horario.hora
                        val totalMs = h.time
                        val horas = (totalMs / 3600000).toInt()
                        val mins = ((totalMs % 3600000) / 60000).toInt()
                        set(Calendar.HOUR_OF_DAY, horas)
                        set(Calendar.MINUTE, mins)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        // Subtract 5 minutes
                        add(Calendar.MINUTE, -5)
                        // If already passed today, schedule tomorrow
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    val intent = Intent(context, DoseReminderReceiver::class.java).apply {
                        putExtra("horario_id", horario.idHorario)
                        putExtra("medicamento_id", horario.idMedicamento)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        horario.idHorario,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setRepeating(
                                    AlarmManager.RTC_WAKEUP,
                                    cal.timeInMillis,
                                    AlarmManager.INTERVAL_DAY,
                                    pendingIntent
                                )
                            }
                        } else {
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                cal.timeInMillis,
                                AlarmManager.INTERVAL_DAY,
                                pendingIntent
                            )
                        }
                    } catch (e: SecurityException) {
                        // Permission not granted
                    }
                }
                return@collect
            }
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Cancel with request codes 0-9999 (covers typical horario IDs)
        for (i in 0..9999) {
            val intent = Intent(context, DoseReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, i, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun isInDoNotDisturbPeriod(context: Context): Boolean {
        val prefs = context.getSharedPreferences("meditrack_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("modo_no_molestar", false)) return false

        val desde = prefs.getString("no_molestar_desde", "22:00") ?: "22:00"
        val hasta = prefs.getString("no_molestar_hasta", "08:00") ?: "08:00"

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val desdeParts = desde.split(":")
        val hastaParts = hasta.split(":")
        val desdeMinutes = (desdeParts[0].toIntOrNull() ?: 22) * 60 + (desdeParts[1].toIntOrNull() ?: 0)
        val hastaMinutes = (hastaParts[0].toIntOrNull() ?: 8) * 60 + (hastaParts[1].toIntOrNull() ?: 0)

        return if (desdeMinutes < hastaMinutes) {
            nowMinutes in desdeMinutes until hastaMinutes
        } else {
            // Crosses midnight
            nowMinutes >= desdeMinutes || nowMinutes < hastaMinutes
        }
    }
}

class DoseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (NotificationHelper.isInDoNotDisturbPeriod(context)) return

        val prefs = context.getSharedPreferences("meditrack_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notificaciones_activas", false)) return

        val horarioId = intent.getIntExtra("horario_id", 0)
        val medicamentoId = intent.getIntExtra("medicamento_id", 0)

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val med = db.medicamentoDao().getMedicamentoPorId(medicamentoId) ?: return@launch

            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Hora de tu medicamento")
                .setContentText("En 5 minutos: ${med.nombreMedicamento} ${med.dosisValor} ${med.formaFarmaceutica}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(horarioId, notification)
        }
    }
}
