package ru.sberdevices.services.published.environment.info.mappers

import ru.sberdevices.services.published.environment.info.models.enums.DreamState
import ru.sberdevices.services.published.environment.info.dtos.enums.DreamState as AidlDreamState

/**
 * Маппер режима сна устройства из AIDL-модуля в режим сна устройства из API-модуля.
 * @author Максим Митюшкин on 12.01.2023
 */
internal interface DreamStateMapper {
    /**
     * Маппит режим сна устройства из AIDL-модуля в режим сна устройства из API-модуля.
     * @param dreamState Режим сна устройства из AIDL-модуля.
     * @return Соответствующий режим сна устройства из API-модуля.
     */
    fun map(dreamState: AidlDreamState): DreamState
}
