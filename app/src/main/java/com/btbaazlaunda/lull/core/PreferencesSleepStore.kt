package com.btbaazlaunda.lull.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.sleepDataStore: DataStore<Preferences> by preferencesDataStore(name = "sleep_mode")

class PreferencesSleepStore(private val dataStore: DataStore<Preferences>) : SleepStore {

    override val state: Flow<SleepState> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSleepState() }
        .distinctUntilChanged()

    override suspend fun setIncluded(feature: Feature, included: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.INCLUDED]?.toFeatures() ?: Feature.DEFAULTS
            prefs[Keys.INCLUDED] = (if (included) current + feature else current - feature).toNames()
        }
    }

    override suspend fun markAsleep(restoreOnWake: Set<Feature>, at: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.SLEEPING] = true
            prefs[Keys.RESTORE_ON_WAKE] = restoreOnWake.toNames()
            prefs[Keys.SLEEP_STARTED_AT] = at
        }
    }

    override suspend fun markAwake(at: Long) {
        dataStore.edit { prefs ->
            val startedAt = prefs[Keys.SLEEP_STARTED_AT] ?: 0L
            if (startedAt in 1..at) prefs[Keys.LAST_SLEEP_DURATION_MS] = at - startedAt
            prefs[Keys.SLEEPING] = false
            prefs.remove(Keys.RESTORE_ON_WAKE)
            prefs.remove(Keys.SLEEP_STARTED_AT)
        }
    }

    private fun Preferences.toSleepState() = SleepState(
        sleeping = this[Keys.SLEEPING] ?: false,
        included = this[Keys.INCLUDED]?.toFeatures() ?: Feature.DEFAULTS,
        restoreOnWake = this[Keys.RESTORE_ON_WAKE]?.toFeatures().orEmpty(),
        sleepStartedAt = this[Keys.SLEEP_STARTED_AT] ?: 0L,
        lastSleepDurationMs = this[Keys.LAST_SLEEP_DURATION_MS] ?: 0L,
    )

    private fun Set<String>.toFeatures(): Set<Feature> = mapNotNullTo(mutableSetOf()) { Feature.fromNameOrNull(it) }

    private fun Set<Feature>.toNames(): Set<String> = mapTo(mutableSetOf()) { it.name }

    private object Keys {
        val SLEEPING = booleanPreferencesKey("sleeping")
        val INCLUDED = stringSetPreferencesKey("included")
        val RESTORE_ON_WAKE = stringSetPreferencesKey("restore_on_wake")
        val SLEEP_STARTED_AT = longPreferencesKey("sleep_started_at")
        val LAST_SLEEP_DURATION_MS = longPreferencesKey("last_sleep_duration_ms")
    }
}
