package com.btbaazlaunda.lull.core

import kotlinx.coroutines.flow.Flow

data class SleepState(
    val sleeping: Boolean = false,
    val included: Set<Feature> = Feature.DEFAULTS,
    /** Features that were awake when sleep began, and so get switched back on waking. */
    val restoreOnWake: Set<Feature> = emptySet(),
    val sleepStartedAt: Long = 0L,
    val lastSleepDurationMs: Long = 0L,
)

interface SleepStore {
    val state: Flow<SleepState>

    suspend fun setIncluded(feature: Feature, included: Boolean)

    suspend fun markAsleep(restoreOnWake: Set<Feature>, at: Long)

    suspend fun markAwake(at: Long)
}
