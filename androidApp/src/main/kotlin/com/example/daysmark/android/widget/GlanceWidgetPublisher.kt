package com.example.daysmark.android.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.daysmark.core.domain.widget.WidgetPublisher
import com.example.daysmark.core.domain.widget.WidgetTimeline
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes each timeline to [WidgetTimelineFile] and redraws the widgets, off the main thread.
 * Returns once the file is written; a failure is logged and rethrown, and the sync tries again with
 * its next timeline.
 */
class GlanceWidgetPublisher(context: Context) : WidgetPublisher {
    private val context = context.applicationContext

    override suspend fun publish(timeline: WidgetTimeline): Unit =
        withContext(Dispatchers.IO) {
            try {
                WidgetTimelineFile(context).write(timeline)
            } catch (failure: IOException) {
                Log.w(TAG, "Couldn't write the widget timeline", failure)
                throw failure
            }
            FocusWidget().updateAll(context)
        }

    private companion object {
        const val TAG = "DaysmarkWidget"
    }
}
