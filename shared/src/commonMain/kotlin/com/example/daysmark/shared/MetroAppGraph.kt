package com.example.daysmark.shared

import androidx.room.RoomDatabase
import com.example.daysmark.core.data.database.DaysmarkDatabase
import com.example.daysmark.core.domain.diagnostics.ErrorReporter
import com.example.daysmark.core.domain.reminders.ReminderScheduler
import com.example.daysmark.core.domain.time.WallClock
import com.example.daysmark.core.domain.widget.WidgetPublisher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

/**
 * The Metro graph behind [AppGraph]. Metro checks it at compile time: a missing or duplicate
 * binding is a build error, not a crash. What a platform must supply is a factory parameter.
 */
@DependencyGraph(AppScope::class)
internal interface MetroAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides databaseBuilder: RoomDatabase.Builder<DaysmarkDatabase>,
            @Provides wallClock: WallClock,
            @Provides reminderScheduler: ReminderScheduler,
            @Provides widgetPublisher: WidgetPublisher,
            @Provides errorReporter: ErrorReporter,
        ): MetroAppGraph
    }
}
