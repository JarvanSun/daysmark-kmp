package com.example.daysmark.android.widget

import android.content.Context
import android.util.Log
import com.example.daysmark.core.domain.widget.WidgetTimeline
import com.example.daysmark.shared.widget.WidgetSnapshot
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** The timeline the app writes and the widget reads, in the same format iOS uses. */
internal class WidgetTimelineFile(context: Context) {
    private val file = File(context.filesDir, WidgetSnapshot.FILE_NAME)

    /** Writes atomically: a temporary file of its own, then one rename over the old file. */
    fun write(timeline: WidgetTimeline) {
        val temporary = File.createTempFile(file.name, ".tmp", file.parentFile)
        try {
            temporary.writeText(WidgetSnapshot.encode(timeline))
            if (!temporary.renameTo(file)) throw IOException("Couldn't replace ${file.name}")
        } finally {
            temporary.delete()
        }
    }

    /**
     * The stored timeline, or `null` when there is none to use: not written yet, unreadable, from
     * another version or damaged. An unreadable file is logged; the widget shows its empty state.
     */
    fun read(): WidgetTimeline? =
        try {
            WidgetSnapshot.decode(file.readText())
        } catch (_: FileNotFoundException) {
            null
        } catch (failure: IOException) {
            Log.w(TAG, "Couldn't read the widget timeline", failure)
            null
        }

    private companion object {
        const val TAG = "DaysmarkWidget"
    }
}
