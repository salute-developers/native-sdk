package ru.sberdevices.messaging

import androidx.annotation.AnyThread
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread

@WorkerThread
interface Messaging {

    /**
     * simple ServerAction example
     * messageName: SERVER_ACTION
     * payload: {"action_id": "GET_STREAM", "parameters": {"content_id": "111111"}}
     *
     * runApp example
     * messageName: RUN_APP
     * payload: {"action_id": "run_app, "app_info": {"projectId":"5633938a-5ff3-49c9-ba7d-fe2a9944de78"}, "parameters": {}}
     *
     * @param serverActionMode need for backend filtering
     *
     * @param stateLevel defines the amount of information gathered by the system.
     *
     * - [StateLevel.ALL] is should be used in most cases.
     *
     * - If your action does not require state gathering, use [StateLevel.WITHOUT_APPS] and action will be sent faster.
     * @return generated message ID that can be used to track response, logs and etc
     */
    fun sendAction(
        messageName: MessageName,
        payload: Payload,
        stateLevel: StateLevel = StateLevel.UNSUPPORTED,
        serverActionMode: ServerActionMode = ServerActionMode.FOREGROUND
    ): MessageId

    /**
     * Send server_action with source app androidApplicationID
     *
     * For internal use only.
     *
     * @param serverActionMode need for backend filtering
     *
     * @param stateLevel defines the amount of information gathered by the system.
     *
     * - [StateLevel.ALL] is should be used in most cases.
     *
     * - If your action does not require state gathering, use [StateLevel.WITHOUT_APPS] and action will be sent faster.
     * @throws SecurityException if trying to call method without having
     * "ru.sberdevices.permission.CROSS_APP_ACTION" permission
     * @return generated message ID that can be used to track response, logs and etc
     */
    @RequiresPermission("ru.sberdevices.permission.CROSS_APP_ACTION")
    fun sendAction(
        messageName: MessageName,
        payload: Payload,
        androidApplicationID: String,
        stateLevel: StateLevel = StateLevel.UNSUPPORTED,
        serverActionMode: ServerActionMode = ServerActionMode.FOREGROUND
    ): MessageId

    /**
     * Send text [text], as if this text was spoken by user.
     */
    fun sendText(text: String)

    /**
     * Add message [listener].
     */
    @AnyThread
    fun addListener(listener: Listener)

    /**
     * Remove message [listener].
     */
    @AnyThread
    fun removeListener(listener: Listener)

    /**
     * Returns device version service.
     * @return [Int.MAX_VALUE] if the service is found on the device,
     * but it does not have a version - in this case, the compatibility of called methods
     * not guaranteed. If the service is not installed on the device, returns null.
     * In all other cases, returns [Int] - the value of the service version.
     */
    fun getVersion(): Int?

    /**
     * Disconnect from service and clear resources.
     */
    @AnyThread
    fun dispose()

    /**
     * Smartapp backend's messages listener.
     */
    @AnyThread
    interface Listener {
        /**
         * New message with id [messageId] and [payload].
         */
        fun onMessage(messageId: MessageId, payload: Payload)

        /**
         * Error from backend [throwable] for message with [messageId].
         */
        fun onError(messageId: MessageId, throwable: Throwable)

        /**
         * Navigation command with [payload] that contains JSON like { "command": "RIGHT" }
         */
        fun onNavigationCommand(payload: Payload) = Unit
    }
}
