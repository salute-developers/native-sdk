package ru.sberdevices.services.assistant

import androidx.annotation.AnyThread
import ru.sberdevices.common.binderhelper.SinceVersion

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
    @SinceVersion(1)
    fun cancelAssistantSpeech(appInfo: String)

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
}
