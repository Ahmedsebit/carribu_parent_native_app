package com.carribu.parentnative.realtime

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class RealtimeTopic(val intervalMillis: Long) {
    LOCATION(10_000),
    ALERTS(20_000),
    CHAT(10_000)
}

interface RealtimeUpdates {
    fun refreshes(topic: RealtimeTopic): Flow<Unit>
}

/**
 * Lifecycle collection starts and stops this polling flow. A Socket.IO
 * implementation can replace it without changing screen ViewModels.
 */
class PollingRealtimeUpdates : RealtimeUpdates {
    override fun refreshes(topic: RealtimeTopic): Flow<Unit> = flow {
        emit(Unit)
        while (true) {
            delay(topic.intervalMillis)
            emit(Unit)
        }
    }
}
