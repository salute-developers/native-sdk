package ru.sberdevices.services.published.environment.info

import kotlinx.coroutines.flow.Flow
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.services.published.environment.info.models.ScreenState
import ru.sberdevices.services.published.environment.info.models.UserSettingsInfo
import ru.sberdevices.services.published.environment.info.models.enums.DeviceType

/**
 * Repository of environment info.
 */
interface EnvironmentInfoRepository {

    /**
     * Flow with current screen state.
     */
    @SinceVersion(1)
    val screenStateFlow: Flow<ScreenState>

    /**
     * Flow with public user settings.
     */
    @SinceVersion(1)
    val userSettingsInfo: Flow<UserSettingsInfo>

    /**
     * Gets StarOS version on device.
     * @return Current StarOS version on device. In situations where
     * version cannot be requested or resolved, returns "unknown".
     */
    @SinceVersion(1)
    suspend fun getStarOsVersion(): String

    /**
     * Gets device type.
     * @return Current device type. In situations where device type
     * cannot be requested or resolved, returns UNKNOWN.
     */
    @SinceVersion(1)
    suspend fun getDeviceType(): DeviceType

    /**
     * Returns device version service.
     * @return [Int.MAX_VALUE] if the service is found on the device,
     * but it does not have a version - in this case, the compatibility of called methods
     * not guaranteed. If the service is not installed on the device, returns null.
     * In all other cases, returns [Int] - the value of the service version.
     */
    fun getVersion(): Int?
}
