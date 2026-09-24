package com.btbaazlaunda.lull.ui

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.shizuku.ShizukuGateway
import com.btbaazlaunda.lull.tile.SleepTileService
import com.btbaazlaunda.lull.widget.SleepWidgetReceiver

fun Context.openShizukuListing() {
    val id = ShizukuGateway.MANAGER_PACKAGE
    tryStart(Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri())) ||
        tryStart(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri()))
}

fun Context.openShizuku() {
    ShizukuGateway.managerLaunchIntent(this)?.let(::tryStart) ?: openShizukuListing()
}

fun Context.openUrl(url: String) {
    tryStart(Intent(Intent.ACTION_VIEW, url.toUri()))
}

/** Asks the launcher to pin the widget; false when the launcher doesn't support it. */
fun Context.requestPinWidget(): Boolean {
    val manager = getSystemService<AppWidgetManager>() ?: return false
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(this, SleepWidgetReceiver::class.java), null, null)
}

val canRequestAddTile: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Shows the system "Add tile" prompt on Android 13+; does nothing on older versions. */
fun Context.requestAddTile() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    getSystemService<StatusBarManager>()?.requestAddTileService(
        ComponentName(this, SleepTileService::class.java),
        getString(R.string.tile_label),
        Icon.createWithResource(this, R.drawable.ic_moon),
        mainExecutor,
    ) { }
}

private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}
