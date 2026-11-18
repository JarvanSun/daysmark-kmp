package com.example.daysmark.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.DaysmarkColors
import com.example.daysmark.android.designsystem.drawable
import com.example.daysmark.android.designsystem.isPastSide
import com.example.daysmark.android.navigation.LaunchRequest
import com.example.daysmark.android.text.phrase
import com.example.daysmark.core.domain.calculation.DayCount
import com.example.daysmark.core.domain.calculation.RangePhase
import com.example.daysmark.core.domain.model.EventIcon
import com.example.daysmark.core.domain.model.EventId
import com.example.daysmark.core.domain.widget.WidgetFocus
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The event in focus on Home, counted from today. The app computes the timeline (now and the
 * following midnights) and this only picks the entry for the current moment and draws it.
 */
class FocusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val timeline = WidgetTimelineFile(context).read()
        val now = Clock.System.now()
        val focus = timeline?.entries?.lastOrNull { it.startsAt <= now }?.focus
        WidgetRefresh.scheduleAfter(context, timeline, now)
        provideContent { FocusContent(context, focus) }
    }

    /** What the widget picker shows (Android 15 and later): a sample event, drawn by the widget. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val sample =
            WidgetFocus(
                id = EventId("preview"),
                title = context.getString(R.string.widget_preview_title),
                iconKey = EventIcon.AIRPLANE.storageKey,
                dayCount = DayCount.Future(PREVIEW_DAYS),
                phase = RangePhase.POINT,
            )
        provideContent { FocusContent(context, sample) }
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    FocusWidget().updateAll(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.daysmark.WIDGET_REFRESH"
    }
}

@Composable
private fun FocusContent(context: Context, focus: WidgetFocus?) {
    val request =
        if (focus == null) LaunchRequest.NewEvent else LaunchRequest.OpenEvent(focus.id.value)
    Column(
        GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(color { canvas })
            .cornerRadius(CORNER_RADIUS.dp)
            .padding(PADDING.dp)
            .clickable(actionStartActivity(request.intent(context)))
    ) {
        val icon = focus?.let { EventIcon.fromStorageKey(it.iconKey) } ?: EventIcon.CALENDAR
        Image(
            ImageProvider(icon.drawable),
            contentDescription = null,
            modifier = GlanceModifier.size(ICON.dp),
        )
        Spacer(GlanceModifier.defaultWeight())
        if (focus == null) {
            Text(context.getString(R.string.widget_empty_title), style = titleStyle())
            Text(
                context.getString(R.string.widget_empty_action),
                style = TextStyle(color { textSecondary }, fontSize = SECONDARY_SIZE.sp),
            )
        } else {
            Text(focus.title, style = titleStyle(), maxLines = 2)
            Text(
                context.resources.phrase(focus.dayCount, focus.phase),
                style =
                    TextStyle(
                        if (focus.dayCount.isPastSide()) color { past } else color { upcoming },
                        fontSize = PHRASE_SIZE.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 2,
            )
        }
    }
}

private fun titleStyle() =
    TextStyle(color { textPrimary }, fontSize = TITLE_SIZE.sp, fontWeight = FontWeight.Medium)

/** A design token in light and dark, which the launcher picks between. */
private fun color(token: DaysmarkColors.() -> Color): ColorProvider =
    ColorProvider(day = DaysmarkColors.Light.token(), night = DaysmarkColors.Dark.token())

private const val PREVIEW_DAYS = 12
private const val CORNER_RADIUS = 24
private const val PADDING = 16
private const val ICON = 36
private const val TITLE_SIZE = 17
private const val PHRASE_SIZE = 15
private const val SECONDARY_SIZE = 14
