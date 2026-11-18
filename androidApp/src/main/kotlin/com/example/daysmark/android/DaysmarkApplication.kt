package com.example.daysmark.android

import android.app.Application
import android.util.Log
import com.example.daysmark.android.reminders.AlarmReminderScheduler
import com.example.daysmark.android.widget.GlanceWidgetPublisher
import com.example.daysmark.android.widget.WidgetPreviews
import com.example.daysmark.shared.AppGraph
import com.example.daysmark.shared.createAppGraph
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DaysmarkApplication : Application() {
    val graph: AppGraph by lazy {
        createAppGraph(
            this,
            AlarmReminderScheduler(this),
            GlanceWidgetPublisher(this),
            LogErrorReporter(),
        )
    }

    /** The process's own optional work; a failure there is logged, never a crash. */
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, failure ->
                    Log.w(TAG, "Background work failed", failure)
                }
        )

    override fun onCreate() {
        super.onCreate()
        // Keeps alarms and the widget in step with the events for as long as the process lives.
        graph.services.start()
        scope.launch { WidgetPreviews.register(this@DaysmarkApplication) }
    }

    private companion object {
        const val TAG = "Daysmark"
    }
}
