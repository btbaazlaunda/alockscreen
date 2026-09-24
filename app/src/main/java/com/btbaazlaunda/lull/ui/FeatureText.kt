package com.btbaazlaunda.lull.ui

import android.content.Context
import androidx.annotation.StringRes
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.core.Feature
import com.btbaazlaunda.lull.core.ToggleOutcome

@get:StringRes
val Feature.labelRes: Int
    get() = when (this) {
        Feature.WIFI -> R.string.feature_wifi
        Feature.MOBILE_DATA -> R.string.feature_mobile_data
        Feature.BLUETOOTH -> R.string.feature_bluetooth
        Feature.BATTERY_SAVER -> R.string.feature_battery_saver
        Feature.DO_NOT_DISTURB -> R.string.feature_dnd
    }

@get:StringRes
val Feature.descriptionRes: Int
    get() = when (this) {
        Feature.WIFI -> R.string.feature_wifi_description
        Feature.MOBILE_DATA -> R.string.feature_mobile_data_description
        Feature.BLUETOOTH -> R.string.feature_bluetooth_description
        Feature.BATTERY_SAVER -> R.string.feature_battery_saver_description
        Feature.DO_NOT_DISTURB -> R.string.feature_dnd_description
    }

/** User-facing explanation of [ToggleOutcome], or null when there is nothing worth saying. */
fun ToggleOutcome.describe(context: Context): String? = when (this) {
    is ToggleOutcome.Switched -> failed.takeIf { it.isNotEmpty() }?.let {
        val template = if (sleeping) R.string.outcome_partial_sleep else R.string.outcome_partial_wake
        context.getString(template, it.joinLabels(context))
    }
    is ToggleOutcome.Failed -> context.getString(R.string.outcome_failed, failed.joinLabels(context))
    ToggleOutcome.SetupRequired -> context.getString(R.string.outcome_setup_required)
    ToggleOutcome.NothingSelected -> context.getString(R.string.outcome_nothing_selected)
}

private fun Set<Feature>.joinLabels(context: Context): String =
    Feature.entries.filter { it in this }.joinToString { context.getString(it.labelRes) }
