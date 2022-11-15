package ru.sberdevices.services.assistant

import androidx.annotation.AnyThread

/**
 * Api to work with smart assistant features on device.
 */
interface PublicAssistantLib {

    /**
     * Cancels current speech from assistant. Only if it was started by the current app.
     * Format of [appInfo] is as following:
     * {
     *  "frontendEndpoint": ...
     *  "appVersionId": ...
     *  "applicationId": ...
     *  "frontendStateId": ...
     *  "projectId": ...
     *  "systemName": ...
     *  "frontendType": "APK"
     * }
     */
    @AnyThread
    fun cancelAssistantSpeech(appInfo: String)

    /**
     * Disconnect from service and clear resources.
     */
    @AnyThread
    fun dispose()
}