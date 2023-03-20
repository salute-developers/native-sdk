package ru.sberdevices.services.published.environment.info.models

import ru.sberdevices.services.published.environment.info.models.enums.DeviceLockMode

/**
 * Values of public user settings.
 * @param isChildModeEnabled Is child mode enabled?
 * @param deviceLockMode Current device lock mode.
 */
data class UserSettingsInfo(
    val isChildModeEnabled: Boolean,
    val deviceLockMode: DeviceLockMode,
)
