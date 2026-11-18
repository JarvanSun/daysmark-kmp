package com.example.daysmark.core.domain.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Brings one system state (pending notifications, the widget's file) in line with the app's data.
 * The only way a sync touches the system: every caller, the long-running sync and a one-off
 * broadcast alike, goes through [reconcile], so applications never interleave and an older result
 * can never land after a newer one (the value is computed inside the same lock).
 *
 * A value is skipped only if it equals the last one *applied successfully*; after a failure the
 * same value is applied again the next time anything asks.
 */
public class Reconciler<T>(
    private val compute: suspend () -> T,
    private val apply: suspend (T) -> Unit,
) {
    private val lock = Mutex()
    private var applied: Applied<T>? = null

    /**
     * Computes the current value and applies it unless it was already applied; [force] applies it
     * anyway (the system may have lost it, as alarms do on reboot). Throws the application's
     * failure; the last applied value then stays what it was.
     */
    public suspend fun reconcile(force: Boolean = false) {
        lock.withLock {
            val value = compute()
            if (!force && applied?.value == value) return
            apply(value)
            applied = Applied(value)
        }
    }

    /** Wraps the value so that `null` can be a value too. */
    private data class Applied<T>(val value: T)
}
