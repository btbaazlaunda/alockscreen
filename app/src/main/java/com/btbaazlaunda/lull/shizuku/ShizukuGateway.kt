package com.btbaazlaunda.lull.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.btbaazlaunda.lull.BuildConfig
import com.btbaazlaunda.lull.core.Feature
import com.btbaazlaunda.lull.core.FeatureSwitcher
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    READY;

    val isRunning: Boolean get() = this != NOT_INSTALLED && this != NOT_RUNNING
}

/**
 * Switches features through [Shizuku](https://shizuku.rikka.app), which grants shell-level access
 * without root. The privileged process is started per toggle and torn down straight after, so it
 * costs nothing while idle.
 */
class ShizukuGateway(private val context: Context) : FeatureSwitcher {

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context, ShellService::class.java))
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener { refresh() }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
    }

    fun refresh() {
        _status.value = readStatus()
    }

    fun requestPermission() {
        if (Shizuku.pingBinder()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    override suspend fun awaitReady(): Boolean {
        refresh()
        if (!status.value.isRunning) {
            // A freshly started process (e.g. from the widget) receives Shizuku's binder asynchronously.
            withTimeoutOrNull(BINDER_WAIT_MS) { status.first { it.isRunning } }
        }
        return status.value == ShizukuStatus.READY
    }

    override suspend fun apply(changes: Map<Feature, Boolean>): Set<Feature> {
        if (changes.isEmpty()) return emptySet()
        val connection = ShellConnection()
        val service = connection.bind() ?: return changes.keys
        return try {
            withContext(Dispatchers.IO) {
                changes.filterNot { (feature, asleep) ->
                    runCatching { service.apply(feature.name, asleep) }.getOrDefault(false)
                }.keys
            }
        } finally {
            connection.unbind()
        }
    }

    private fun readStatus(): ShizukuStatus = runCatching {
        when {
            !Shizuku.pingBinder() -> if (isManagerInstalled()) ShizukuStatus.NOT_RUNNING else ShizukuStatus.NOT_INSTALLED
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.READY
            Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStatus.PERMISSION_DENIED
            else -> ShizukuStatus.PERMISSION_REQUIRED
        }
    }.getOrDefault(ShizukuStatus.NOT_RUNNING)

    private fun isManagerInstalled(): Boolean = managerLaunchIntent(context) != null

    private inner class ShellConnection : ServiceConnection {
        private var pending: CancellableContinuation<IShellService?>? = null

        suspend fun bind(): IShellService? = withTimeoutOrNull(SERVICE_BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                pending = continuation
                runCatching { Shizuku.bindUserService(serviceArgs, this@ShellConnection) }
                    .onFailure { deliver(null) }
            }
        }

        fun unbind() {
            runCatching { Shizuku.unbindUserService(serviceArgs, this, true) }
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            deliver(binder?.takeIf { it.pingBinder() }?.let(IShellService.Stub::asInterface))
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit

        private fun deliver(service: IShellService?) {
            pending?.takeIf { it.isActive }?.resume(service)
            pending = null
        }
    }

    companion object {
        const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"
        const val SETUP_GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"

        private const val PERMISSION_REQUEST_CODE = 1
        private const val BINDER_WAIT_MS = 3_000L
        private const val SERVICE_BIND_TIMEOUT_MS = 6_000L

        fun managerLaunchIntent(context: Context): Intent? =
            context.packageManager.getLaunchIntentForPackage(MANAGER_PACKAGE)
    }
}
