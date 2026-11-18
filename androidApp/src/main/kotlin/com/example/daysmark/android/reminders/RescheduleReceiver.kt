package com.example.daysmark.android.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.daysmark.android.DaysmarkApplication
import com.example.daysmark.shared.replanAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Plans reminders and the widget again when alarms were lost (reboot) or their wall-clock times
 * moved (time, zone or language change), without the app's screens open.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        val graph = (context.applicationContext as DaysmarkApplication).graph
        CoroutineScope(Dispatchers.Main).launch {
            try {
                graph.services.replanAll()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        // A compile-time constant: older versions simply never send it.
        @SuppressLint("InlinedApi")
        val ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
    }
}
