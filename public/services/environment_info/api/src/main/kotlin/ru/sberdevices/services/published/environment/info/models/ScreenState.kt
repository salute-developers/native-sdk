package ru.sberdevices.services.published.environment.info.models

import ru.sberdevices.services.published.environment.info.models.enums.DreamState

/**
 * State of device screen.
 * @property isNoScreenModeEnabled Is no screen mode enabled?
 * @property dreamState Current device dream state.
 */
data class ScreenState(
    val isNoScreenModeEnabled: Boolean,
    val dreamState: DreamState,
)
