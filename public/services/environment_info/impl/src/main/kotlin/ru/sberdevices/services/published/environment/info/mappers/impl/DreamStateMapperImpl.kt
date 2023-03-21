package ru.sberdevices.services.published.environment.info.mappers.impl

import ru.sberdevices.services.published.environment.info.mappers.DreamStateMapper
import ru.sberdevices.services.published.environment.info.models.enums.DreamState
import ru.sberdevices.services.published.environment.info.dtos.enums.DreamState as AidlDreamState

/**
 * Реализация [DreamStateMapper].
 * @author Максим Митюшкин on 12.01.2023
 */
internal class DreamStateMapperImpl : DreamStateMapper {

    override fun map(dreamState: AidlDreamState): DreamState {
        return when (dreamState) {
            AidlDreamState.AWAKE -> DreamState.AWAKE
            AidlDreamState.NIGHT -> DreamState.NIGHT
            AidlDreamState.SCREENSAVER -> DreamState.SCREENSAVER
        }
    }
}
