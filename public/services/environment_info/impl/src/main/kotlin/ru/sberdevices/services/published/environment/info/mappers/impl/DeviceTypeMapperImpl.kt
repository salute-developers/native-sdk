package ru.sberdevices.services.published.environment.info.mappers.impl

import ru.sberdevices.services.published.environment.info.mappers.DeviceTypeMapper
import ru.sberdevices.services.published.environment.info.models.enums.DeviceType
import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceType as AidlDeviceType

/**
 * Реализация [DeviceTypeMapper].
 * @author Максим Митюшкин on 12.01.2023
 */
internal class DeviceTypeMapperImpl : DeviceTypeMapper {

    override fun map(deviceType: AidlDeviceType): DeviceType {
        return when (deviceType) {
            AidlDeviceType.STARGATE -> DeviceType.STARGATE
            AidlDeviceType.SBERBOX -> DeviceType.SBERBOX
            AidlDeviceType.SMARTBOX -> DeviceType.SMARTBOX
            AidlDeviceType.SBERBOX_TOP -> DeviceType.SBERBOX_TOP
            AidlDeviceType.TV -> DeviceType.TV
            AidlDeviceType.HEAD_UNIT -> DeviceType.HEAD_UNIT
            AidlDeviceType.BOOM -> DeviceType.BOOM
            AidlDeviceType.BOOM_MINI -> DeviceType.BOOM_MINI
            AidlDeviceType.SBER_TIME -> DeviceType.SBER_TIME
            AidlDeviceType.UNKNOWN -> DeviceType.UNKNOWN
        }
    }
}
