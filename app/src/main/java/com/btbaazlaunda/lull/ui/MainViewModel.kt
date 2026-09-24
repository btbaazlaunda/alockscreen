package com.btbaazlaunda.lull.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.btbaazlaunda.lull.LullApplication
import com.btbaazlaunda.lull.core.DeviceState
import com.btbaazlaunda.lull.core.Feature
import com.btbaazlaunda.lull.core.SleepController
import com.btbaazlaunda.lull.core.SleepStore
import com.btbaazlaunda.lull.core.ToggleOutcome
import com.btbaazlaunda.lull.shizuku.ShizukuGateway
import com.btbaazlaunda.lull.shizuku.ShizukuStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeatureToggle(val feature: Feature, val included: Boolean)

data class HomeUiState(
    val loaded: Boolean = false,
    val sleeping: Boolean = false,
    val sleepStartedAt: Long = 0L,
    val lastSleepDurationMs: Long = 0L,
    val features: List<FeatureToggle> = emptyList(),
    val shizuku: ShizukuStatus = ShizukuStatus.READY,
    val busy: Boolean = false,
    val outcome: ToggleOutcome? = null,
)

class MainViewModel(
    private val store: SleepStore,
    private val controller: SleepController,
    private val shizuku: ShizukuGateway,
    device: DeviceState,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val outcome = MutableStateFlow<ToggleOutcome?>(null)
    private val supported = Feature.entries.filter(device::isSupported)

    val uiState: StateFlow<HomeUiState> =
        combine(store.state, shizuku.status, busy, outcome) { sleep, setup, isBusy, lastOutcome ->
            HomeUiState(
                loaded = true,
                sleeping = sleep.sleeping,
                sleepStartedAt = sleep.sleepStartedAt,
                lastSleepDurationMs = sleep.lastSleepDurationMs,
                features = supported.map { FeatureToggle(it, it in sleep.included) },
                shizuku = setup,
                busy = isBusy,
                outcome = lastOutcome,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleSleep() {
        if (!busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                // Runs in the app scope so leaving the screen can't interrupt a half-applied switch.
                outcome.value = appScope.async { controller.toggle() }.await()
            } finally {
                busy.value = false
            }
        }
    }

    fun setIncluded(feature: Feature, included: Boolean) {
        viewModelScope.launch { store.setIncluded(feature, included) }
    }

    fun refreshSetup() = shizuku.refresh()

    fun requestShizukuPermission() = shizuku.requestPermission()

    fun outcomeShown() {
        outcome.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as LullApplication).container
                MainViewModel(
                    store = container.store,
                    controller = container.controller,
                    shizuku = container.shizuku,
                    device = container.deviceState,
                    appScope = container.appScope,
                )
            }
        }
    }
}
