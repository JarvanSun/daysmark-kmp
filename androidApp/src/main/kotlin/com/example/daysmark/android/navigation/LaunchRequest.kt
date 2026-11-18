package com.example.daysmark.android.navigation

import android.content.Context
import android.content.Intent
import com.example.daysmark.android.MainActivity

/** What a notification or the widget asks the app to open when it launches it. */
sealed interface LaunchRequest {
    data class OpenEvent(val eventId: String) : LaunchRequest

    data object NewEvent : LaunchRequest

    /** An intent that opens [MainActivity] with this request, reusing the running instance. */
    fun intent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply {
                when (this@LaunchRequest) {
                    is OpenEvent -> putExtra(EXTRA_EVENT_ID, eventId)
                    NewEvent -> putExtra(EXTRA_NEW_EVENT, true)
                }
            }

    companion object {
        private const val ACTION = "com.example.daysmark.OPEN"
        private const val EXTRA_EVENT_ID = "eventId"
        private const val EXTRA_NEW_EVENT = "newEvent"

        fun from(intent: Intent?): LaunchRequest? =
            when {
                intent == null -> null
                intent.getBooleanExtra(EXTRA_NEW_EVENT, false) -> NewEvent
                else -> intent.getStringExtra(EXTRA_EVENT_ID)?.let(::OpenEvent)
            }
    }
}
