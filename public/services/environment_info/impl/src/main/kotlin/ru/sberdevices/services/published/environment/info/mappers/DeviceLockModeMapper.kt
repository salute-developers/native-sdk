package ru.sberdevices.services.published.environment.info.mappers

import ru.sberdevices.services.published.environment.info.models.enums.DeviceLockMode
import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceLockMode as AidlDeviceLockMode

/**
 * Маппер режима блокировки устройства из AIDL-модуля в режим блокировки устройства из API-модуля.
 * @author Максим Митюшкин on 12.01.2023
 */
internal interface DeviceLockModeMapper {
    /**
     * Маппит режим блокировки устройства из AIDL-модуля в режим блокировки устройства из API-модуля.
     * @param deviceLockMode Режим блокировки устройства из AIDL-модуля.
     * @return Соответствующий режим блокировки устройства из API-модуля.
     */
    fun map(deviceLockMode: AidlDeviceLockMode): DeviceLockMode
}
