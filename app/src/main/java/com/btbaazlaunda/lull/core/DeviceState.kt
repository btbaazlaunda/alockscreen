package com.btbaazlaunda.lull.core

interface DeviceState {
    fun isSupported(feature: Feature): Boolean

    /** Whether [feature] is in its daytime state: radio on, battery saver or Do Not Disturb off. */
    fun isAwake(feature: Feature): Boolean
}
