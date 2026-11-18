package com.example.daysmark.core.testing

import com.example.daysmark.core.domain.model.Event
import com.example.daysmark.core.domain.repository.OnboardingRepository
import com.example.daysmark.core.domain.repository.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * An in-memory [OnboardingRepository]; set [failure] to make the next [complete] throw it, or
 * [failReads] to make reading the flag fail.
 */
public class FakeOnboardingRepository : OnboardingRepository {
    public val isCompleted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    public val savedEvents: MutableList<Event> = mutableListOf()
    public var failure: Exception? = null
    public var failReads: Boolean = false

    override fun observeIsCompleted(): Flow<Boolean> = flow {
        if (failReads) throw StorageException("The database couldn't be read")
        emitAll(isCompleted)
    }

    override suspend fun complete(events: List<Event>) {
        failure?.let {
            failure = null
            throw it
        }
        savedEvents += events
        isCompleted.value = true
    }
}
