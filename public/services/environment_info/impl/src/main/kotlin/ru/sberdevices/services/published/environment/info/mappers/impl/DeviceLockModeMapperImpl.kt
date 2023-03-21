package ru.sberdevices.services.published.environment.info.mappers.impl

import ru.sberdevices.services.published.environment.info.mappers.DeviceLockModeMapper
import ru.sberdevices.services.published.environment.info.models.enums.DeviceLockMode
import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceLockMode as AidlDeviceLockMode

/**
 * Реализация [DeviceLockModeMapper].
 * @author Максим Митюшкин on 12.01.2023
 */
internal class DeviceLockModeMapperImpl : DeviceLockModeMapper {

    override fun map(deviceLockMode: AidlDeviceLockMode): DeviceLockMode {
        return when (deviceLockMode) {
            AidlDeviceLockMode.NO_LOCK -> DeviceLockMode.NO_LOCK
            AidlDeviceLockMode.PIN_CODE -> DeviceLockMode.PIN_CODE
        }
    }
}
