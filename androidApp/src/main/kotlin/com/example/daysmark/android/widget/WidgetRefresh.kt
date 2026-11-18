package com.example.daysmark.android.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.example.daysmark.core.domain.widget.WidgetTimeline
import kotlin.time.Instant

/**
 * Wakes the widget when its next entry starts (the next midnight), so it moves on even when the app
 * isn't running. Inexact: a few minutes late is fine, and it needs no exact-alarm permission.
 */
internal object WidgetRefresh {
    private const val WINDOW_MS = 10 * 60 * 1_000L

    fun scheduleAfter(context: Context, timeline: WidgetTimeline?, now: Instant) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val intent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, FocusWidgetReceiver::class.java)
                    .setAction(FocusWidgetReceiver.ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val next = timeline?.entries?.firstOrNull { it.startsAt > now }?.startsAt
        if (next == null) {
            alarms.cancel(intent)
        } else {
            alarms.setWindow(AlarmManager.RTC, next.toEpochMilliseconds(), WINDOW_MS, intent)
        }
    }
}
