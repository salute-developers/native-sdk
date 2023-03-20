package ru.sberdevices.services.published.environment.info.dtos;

import ru.sberdevices.services.published.environment.info.dtos.enums.DreamState;

parcelable ScreenStateDto {
    boolean isNoScreenModeEnabled;
    DreamState dreamState;
}
