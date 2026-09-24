package com.btbaazlaunda.lull.core

import com.btbaazlaunda.lull.core.Feature.BATTERY_SAVER
import com.btbaazlaunda.lull.core.Feature.BLUETOOTH
import com.btbaazlaunda.lull.core.Feature.DO_NOT_DISTURB
import com.btbaazlaunda.lull.core.Feature.MOBILE_DATA
import com.btbaazlaunda.lull.core.Feature.WIFI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepControllerTest {

    private val store = FakeSleepStore()
    private val device = FakeDeviceState()
    private val switcher = FakeSwitcher(device)
    private var now = 1_000L
    private var stateChanges = 0
    private val controller = SleepController(
        store = store,
        switcher = switcher,
        device = device,
        onStateChanged = { stateChanges++ },
        clock = { now },
    )

    @Test
    fun sleep_switchesOnlyWhatIsAwake_andRemembersIt() = runTest {
        device.awake -= MOBILE_DATA // user already had mobile data off

        val outcome = controller.toggle()

        assertEquals(ToggleOutcome.Switched(sleeping = true), outcome)
        assertEquals(listOf(mapOf(WIFI to true, BLUETOOTH to true, BATTERY_SAVER to true)), switcher.calls)
        assertEquals(setOf(WIFI, BLUETOOTH, BATTERY_SAVER), store.value.restoreOnWake)
        assertTrue(store.value.sleeping)
        assertEquals(1, stateChanges)
    }

    @Test
    fun wake_restoresOnlyRememberedFeatures_inReverseOrder() = runTest {
        device.awake -= MOBILE_DATA
        controller.toggle()
        now = 5_000L

        val outcome = controller.toggle()

        assertEquals(ToggleOutcome.Switched(sleeping = false), outcome)
        assertEquals(listOf(BATTERY_SAVER, BLUETOOTH, WIFI), switcher.calls.last().keys.toList())
        assertTrue(switcher.calls.last().values.none { it })
        assertFalse(MOBILE_DATA in device.awake)
        assertFalse(store.value.sleeping)
    }

    @Test
    fun sleep_withoutShizuku_requiresSetup_andChangesNothing() = runTest {
        switcher.ready = false

        assertEquals(ToggleOutcome.SetupRequired, controller.toggle())
        assertTrue(switcher.calls.isEmpty())
        assertFalse(store.value.sleeping)
        assertEquals(0, stateChanges)
    }

    @Test
    fun sleep_withNothingSelected_doesNothing() = runTest {
        store.value = store.value.copy(included = emptySet())

        assertEquals(ToggleOutcome.NothingSelected, controller.toggle())
        assertFalse(store.value.sleeping)
    }

    @Test
    fun sleep_skipsUnsupportedFeatures() = runTest {
        device.unsupported += MOBILE_DATA

        controller.toggle()

        assertFalse(MOBILE_DATA in switcher.calls.single())
    }

    @Test
    fun sleep_whenEverySwitchFails_staysAwake() = runTest {
        switcher.failing = Feature.entries.toSet()

        val outcome = controller.toggle()

        assertEquals(ToggleOutcome.Failed(setOf(WIFI, MOBILE_DATA, BLUETOOTH, BATTERY_SAVER)), outcome)
        assertFalse(store.value.sleeping)
    }

    @Test
    fun sleep_partialFailure_isReported_andNotRestoredLater() = runTest {
        switcher.failing = setOf(BLUETOOTH)

        val outcome = controller.toggle()

        assertEquals(ToggleOutcome.Switched(sleeping = true, failed = setOf(BLUETOOTH)), outcome)
        assertFalse(BLUETOOTH in store.value.restoreOnWake)
    }

    @Test
    fun wake_withoutShizuku_stillWakes_andReportsWhatWasNotRestored() = runTest {
        store.value = store.value.copy(included = setOf(WIFI, DO_NOT_DISTURB))
        controller.toggle()
        switcher.ready = false

        val outcome = controller.toggle()

        assertEquals(ToggleOutcome.Switched(sleeping = false, failed = setOf(WIFI, DO_NOT_DISTURB)), outcome)
        assertFalse(store.value.sleeping)
    }
}

private class FakeSleepStore : SleepStore {
    private val flow = MutableStateFlow(SleepState())
    var value: SleepState
        get() = flow.value
        set(state) {
            flow.value = state
        }

    override val state = flow

    override suspend fun setIncluded(feature: Feature, included: Boolean) {
        value = value.copy(included = if (included) value.included + feature else value.included - feature)
    }

    override suspend fun markAsleep(restoreOnWake: Set<Feature>, at: Long) {
        value = value.copy(sleeping = true, restoreOnWake = restoreOnWake, sleepStartedAt = at)
    }

    override suspend fun markAwake(at: Long) {
        value = value.copy(sleeping = false, restoreOnWake = emptySet(), lastSleepDurationMs = at - value.sleepStartedAt)
    }
}

private class FakeDeviceState : DeviceState {
    val awake = Feature.entries.toMutableSet()
    val unsupported = mutableSetOf<Feature>()

    override fun isSupported(feature: Feature) = feature !in unsupported

    override fun isAwake(feature: Feature) = feature in awake
}

private class FakeSwitcher(private val device: FakeDeviceState) : FeatureSwitcher {
    var ready = true
    var failing = emptySet<Feature>()
    val calls = mutableListOf<Map<Feature, Boolean>>()

    override suspend fun awaitReady() = ready

    override suspend fun apply(changes: Map<Feature, Boolean>): Set<Feature> {
        calls += changes
        for ((feature, asleep) in changes) {
            if (feature in failing) continue
            if (asleep) device.awake.remove(feature) else device.awake.add(feature)
        }
        return changes.keys intersect failing
    }
}
