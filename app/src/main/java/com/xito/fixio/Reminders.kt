package com.xito.fixio

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

object Reminders {
    const val CHANNEL = "fixio_daily"

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Recordatorio diario", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Te recuerda registrar cómo te encuentras hoy"
                }
            )
        }
    }

    fun schedule(c: Context) {
        ensureChannel(c)
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            c, 1001, Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Store.reminderHour(c))
            set(Calendar.MINUTE, Store.reminderMin(c))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Reminders.ensureChannel(c)
        val already = Store.entries(c).any { it.date == Store.today() }
        if (already) return
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, Reminders.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("¿Cómo te encuentras hoy?")
            .setContentText("Registra tus zonas de dolor en Fixio y mantén tu racha 🔥")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try { nm.notify(2001, n) } catch (_: SecurityException) {}
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) Reminders.schedule(c)
    }
}
