package com.example.daysmark.android.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.reminders.PlannedReminder
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Schedules the shared reminder plan with AlarmManager. Exact alarms when the user allows them,
 * otherwise the system's nearest window. Alarms don't survive a reboot; [RescheduleReceiver] plans
 * again then.
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val store = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    /**
     * Replaces the alarms in a way a failure halfway can't leave untracked: the record first
     * becomes every alarm that may exist (the old ones and the new ones), then the alarms change,
     * then the record shrinks to the new ones. After a failure the record still names every alarm
     * that might be set, so the next plan cancels them all.
     */
    override suspend fun replaceAll(reminders: List<PlannedReminder>): Unit =
        withContext(Dispatchers.IO) {
            val previous = store.getStringSet(SCHEDULED, emptySet()).orEmpty()
            val next = reminders.map { it.id }.toSet()
            try {
                record(previous + next)
                previous.forEach { id ->
                    alarms.cancel(pendingIntent(id, Intent(context, ReminderReceiver::class.java)))
                }
                reminders.forEach(::schedule)
                record(next)
            } catch (failure: SecurityException) {
                // Exact alarms were revoked mid-way; the permission change replans.
                Log.w(TAG, "Couldn't schedule reminders", failure)
                throw failure
            } catch (failure: IOException) {
                Log.w(TAG, "Couldn't record the scheduled reminders", failure)
                throw failure
            }
        }

    /** Writes the set of alarms that may be set, synchronously; a failed write is an error. */
    // Not the KTX edit {}: it hides commit()'s result, which is the point here.
    @SuppressLint("UseKtx")
    private fun record(ids: Set<String>) {
        if (!store.edit().putStringSet(SCHEDULED, ids).commit()) {
            throw IOException("Couldn't record ${ids.size} reminders")
        }
    }

    private fun schedule(reminder: PlannedReminder) {
        val intent =
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_EVENT_ID, reminder.eventId.value)
                .putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
                .putExtra(
                    ReminderReceiver.EXTRA_BODY,
                    context.resources.phrase(reminder.dayCount, reminder.phase),
                )
                .putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, reminder.id.hashCode())
        val operation = pendingIntent(reminder.id, intent)
        // Wall-clock time: the reminder fires at that local time in the zone the user is in now.
        val at =
            LocalDateTime(
                    reminder.date.year,
                    reminder.date.month,
                    reminder.date.day,
                    reminder.time.hour,
                    reminder.time.minute,
                )
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        // Before Android 12 every app may set exact alarms; from 12 on, only with the user's leave.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun pendingIntent(id: String, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            // A distinct data URI per reminder keeps their pending intents apart.
            intent.setData("daysmark://reminder/${Uri.encode(id)}".toUri()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val TAG = "DaysmarkReminders"
        const val STORE = "reminders"
        const val SCHEDULED = "scheduled"
    }
}
