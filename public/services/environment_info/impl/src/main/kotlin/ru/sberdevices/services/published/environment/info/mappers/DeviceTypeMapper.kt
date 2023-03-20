package ru.sberdevices.services.published.environment.info.mappers

import ru.sberdevices.services.published.environment.info.models.enums.DeviceType
import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceType as AidlDeviceType

/**
 * Маппер типа устройства из AIDL-модуля в тип устройства из API-модуля.
 * @author Максим Митюшкин on 12.01.2023
 */
internal interface DeviceTypeMapper {
    /**
     * Маппит тип устройства из AIDL-модуля в тип устройства из API-модуля.
     * @param deviceType Тип устройства из AIDL-модуля.
     * @return Соответствующий тип устройства из API-модуля.
     */
    fun map(deviceType: AidlDeviceType): DeviceType
}
