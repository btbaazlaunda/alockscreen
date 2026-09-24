package com.btbaazlaunda.lull.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Something that can move features between their day and night states. */
interface FeatureSwitcher {
    /** Waits briefly for the backend to become usable; false means the user must finish setup. */
    suspend fun awaitReady(): Boolean

    /** Moves each feature to its night (`true`) or day (`false`) state and returns the ones that failed. */
    suspend fun apply(changes: Map<Feature, Boolean>): Set<Feature>
}

sealed interface ToggleOutcome {
    /** Sleep mode changed; [failed] lists anything that could not be switched. */
    data class Switched(val sleeping: Boolean, val failed: Set<Feature> = emptySet()) : ToggleOutcome

    /** Nothing could be switched, so sleep mode stayed off. */
    data class Failed(val failed: Set<Feature>) : ToggleOutcome

    data object SetupRequired : ToggleOutcome

    data object NothingSelected : ToggleOutcome
}

/**
 * Single entry point for the app, widget and Quick Settings tile.
 *
 * Going to sleep remembers which features were awake and only switches those, so waking
 * restores the phone to exactly how it was rather than blindly turning everything on.
 */
class SleepController(
    private val store: SleepStore,
    private val switcher: FeatureSwitcher,
    private val device: DeviceState,
    private val onStateChanged: suspend () -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun toggle(): ToggleOutcome = mutex.withLock {
        val state = store.state.first()
        if (state.sleeping) wake(state) else sleep(state)
    }

    private suspend fun sleep(state: SleepState): ToggleOutcome {
        val targets = Feature.entries.filter { it in state.included && device.isSupported(it) }
        if (targets.isEmpty()) return ToggleOutcome.NothingSelected
        if (!switcher.awaitReady()) return ToggleOutcome.SetupRequired

        val toSwitch = targets.filter(device::isAwake)
        val failed = switcher.apply(toSwitch.associateWith { true })
        if (toSwitch.isNotEmpty() && failed.containsAll(toSwitch)) return ToggleOutcome.Failed(failed)

        store.markAsleep(restoreOnWake = toSwitch.toSet() - failed, at = clock())
        onStateChanged()
        return ToggleOutcome.Switched(sleeping = true, failed = failed)
    }

    private suspend fun wake(state: SleepState): ToggleOutcome {
        val toRestore = Feature.entries.reversed().filter { it in state.restoreOnWake }
        val failed = when {
            toRestore.isEmpty() -> emptySet()
            switcher.awaitReady() -> switcher.apply(toRestore.associateWith { false })
            else -> toRestore.toSet()
        }
        // Waking always completes so sleep mode can never get stuck on; failures are reported instead.
        store.markAwake(at = clock())
        onStateChanged()
        return ToggleOutcome.Switched(sleeping = false, failed = failed)
    }
}
