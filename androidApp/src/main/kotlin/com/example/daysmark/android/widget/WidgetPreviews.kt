package com.example.daysmark.android.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Registers the widget picker's preview, rendered from [FocusWidget.providePreview] (Android 15 and
 * later). Optional: without it the picker shows the app icon, so a failure is only logged.
 */
internal object WidgetPreviews {
    suspend fun register(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val result = GlanceAppWidgetManager(context).setWidgetPreviews(FocusWidgetReceiver::class)
        if (result != GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            Log.i("DaysmarkWidget", "The widget preview wasn't registered (result $result)")
        }
    }
}
