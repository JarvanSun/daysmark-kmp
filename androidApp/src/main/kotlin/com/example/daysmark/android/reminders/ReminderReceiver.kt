package com.example.daysmark.android.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.daysmark.android.R
import com.example.daysmark.android.navigation.LaunchRequest

/**
 * Posts a reminder: the event's title and the phrase for that moment. Tapping it opens the event.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        ensureChannel(context)
        val open =
            PendingIntent.getActivity(
                context,
                eventId.hashCode(),
                LaunchRequest.OpenEvent(eventId).intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(intent.getStringExtra(EXTRA_TITLE))
                .setContentText(intent.getStringExtra(EXTRA_BODY))
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0), notification)
    }

    companion object {
        const val EXTRA_EVENT_ID = "eventId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        private const val CHANNEL = "reminders"

        fun ensureChannel(context: Context) {
            val channel =
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.reminders_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            context
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
