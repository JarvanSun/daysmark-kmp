package com.example.daysmark.feature.events

import kotlinx.coroutines.flow.SharingStarted

/**
 * How screens that only show stored data share their state: kept for five seconds after the last
 * subscriber leaves, so a rotation or a sheet over the screen doesn't restart the database queries.
 * Screens holding unsaved input (the editor) share eagerly instead.
 */
internal val WhileOnScreen: SharingStarted = SharingStarted.WhileSubscribed(5_000)
