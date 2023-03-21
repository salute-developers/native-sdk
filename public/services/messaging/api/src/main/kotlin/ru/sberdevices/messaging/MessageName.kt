package ru.sberdevices.messaging

import androidx.annotation.RequiresPermission

enum class MessageName {
    /**
     * Request to your own backend.
     */
    SERVER_ACTION,

    /**
     * For opening up another app.
     */
    RUN_APP,

    /**
     * For opening up another app with specific deeplink format.
     */
    RUN_APP_DEEPLINK,

    /**
     * To send some statistics.
     */
    HEARTBEAT,

    /**
     * Getting token for ihub api.
     */
    @RequiresPermission("ru.sberdevices.permission.GET_IHUB_TOKEN")
    GET_IHUB_TOKEN,

    /**
     * Update IP.
     */
    @RequiresPermission("ru.sberdevices.permission.IP_UPDATE")
    UPDATE_IP,

    /**
     * Close app.
     */
    CLOSE_APP
}
