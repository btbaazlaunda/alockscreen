package com.btbaazlaunda.lull.core

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService

/** Reads state through public APIs; only switching needs elevated access. */
class AndroidDeviceState(context: Context) : DeviceState {
    private val packageManager = context.packageManager
    private val wifi = context.getSystemService<WifiManager>()
    private val telephony = context.getSystemService<TelephonyManager>()
    private val bluetooth = context.getSystemService<BluetoothManager>()
    private val power = context.getSystemService<PowerManager>()
    private val notifications = context.getSystemService<NotificationManager>()

    override fun isSupported(feature: Feature): Boolean = when (feature) {
        Feature.WIFI -> packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        Feature.MOBILE_DATA -> packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        Feature.BLUETOOTH -> packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        Feature.BATTERY_SAVER, Feature.DO_NOT_DISTURB -> true
    }

    override fun isAwake(feature: Feature): Boolean = when (feature) {
        Feature.WIFI -> wifi?.isWifiEnabled ?: false
        Feature.MOBILE_DATA -> isMobileDataOn()
        Feature.BLUETOOTH -> bluetooth?.adapter?.isEnabled ?: false
        Feature.BATTERY_SAVER -> power?.isPowerSaveMode != true
        Feature.DO_NOT_DISTURB -> isDoNotDisturbOff()
    }

    private fun isDoNotDisturbOff(): Boolean {
        val filter = notifications?.currentInterruptionFilter ?: return true
        return when (filter) {
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> true
            else -> false
        }
    }

    // ACCESS_NETWORK_STATE is enough on Android 11+, but some OEM builds still demand
    // READ_PHONE_STATE. Unknown counts as on, so waking errs toward reconnecting.
    private fun isMobileDataOn(): Boolean = try {
        telephony?.isDataEnabled ?: true
    } catch (_: SecurityException) {
        true
    }
}
