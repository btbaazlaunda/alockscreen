package com.btbaazlaunda.lull

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.btbaazlaunda.lull.core.AndroidDeviceState
import com.btbaazlaunda.lull.core.DeviceState
import com.btbaazlaunda.lull.core.PreferencesSleepStore
import com.btbaazlaunda.lull.core.SleepController
import com.btbaazlaunda.lull.core.SleepStore
import com.btbaazlaunda.lull.core.sleepDataStore
import com.btbaazlaunda.lull.shizuku.ShizukuGateway
import com.btbaazlaunda.lull.widget.SleepWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LullApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency graph; small enough that a DI framework would only add weight. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Outlives screens so a toggle started from the UI, widget or tile always finishes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val store: SleepStore = PreferencesSleepStore(appContext.sleepDataStore)
    val deviceState: DeviceState = AndroidDeviceState(appContext)
    val shizuku = ShizukuGateway(appContext)
    val controller = SleepController(
        store = store,
        switcher = shizuku,
        device = deviceState,
        onStateChanged = { SleepWidget().updateAll(appContext) },
    )
}

val Context.appContainer: AppContainer
    get() = (applicationContext as LullApplication).container
