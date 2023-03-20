package ru.sberdevices.services.published.environment.info.dtos;

import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceLockMode;

parcelable UserSettingsInfoDto {
    boolean isChildModeEnabled;
    DeviceLockMode deviceLockMode;
}
