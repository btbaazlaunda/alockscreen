package com.btbaazlaunda.lull.shizuku

import com.btbaazlaunda.lull.core.Feature
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shizuku user service. It runs in a separate process with the shell user's privileges, which is
 * what lets it switch radios that ordinary apps may not touch since Android 10. It only ever runs
 * the fixed commands below, never caller-supplied input.
 */
class ShellService : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun apply(feature: String, asleep: Boolean): Boolean {
        val target = Feature.fromNameOrNull(feature) ?: return false
        return run(commandFor(target, asleep)) == 0
    }

    private fun run(command: List<String>): Int {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        return try {
            if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.exitValue() else -1
        } finally {
            process.destroy()
        }
    }

    internal companion object {
        private const val TIMEOUT_SECONDS = 10L
        private const val SVC = "/system/bin/svc"
        private const val CMD = "/system/bin/cmd"

        fun commandFor(feature: Feature, asleep: Boolean): List<String> = when (feature) {
            Feature.WIFI -> listOf(SVC, "wifi", if (asleep) "disable" else "enable")
            Feature.MOBILE_DATA -> listOf(SVC, "data", if (asleep) "disable" else "enable")
            Feature.BLUETOOTH -> listOf(SVC, "bluetooth", if (asleep) "disable" else "enable")
            Feature.BATTERY_SAVER -> listOf(CMD, "power", "set-mode", if (asleep) "1" else "0")
            Feature.DO_NOT_DISTURB -> listOf(CMD, "notification", "set_dnd", if (asleep) "priority" else "off")
        }
    }
}
