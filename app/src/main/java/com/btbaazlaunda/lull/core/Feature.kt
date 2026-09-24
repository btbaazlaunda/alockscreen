package com.btbaazlaunda.lull.core

/**
 * Something sleep mode switches. Radios are turned off while asleep;
 * battery saver and Do Not Disturb are turned on.
 *
 * Declaration order is the order changes are applied when going to sleep
 * (and reversed when waking).
 */
enum class Feature {
    WIFI,
    MOBILE_DATA,
    BLUETOOTH,
    BATTERY_SAVER,
    DO_NOT_DISTURB;

    companion object {
        /** What a fresh install switches. Do Not Disturb is opt-in. */
        val DEFAULTS: Set<Feature> = setOf(WIFI, MOBILE_DATA, BLUETOOTH, BATTERY_SAVER)

        fun fromNameOrNull(name: String): Feature? = entries.firstOrNull { it.name == name }
    }
}
