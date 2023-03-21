package ru.sberdevices.services.published.environment.info.listeners;

import ru.sberdevices.services.published.environment.info.dtos.UserSettingsInfoDto;

interface IUserSettingsListener {
    oneway void onUserSettingsChange(in UserSettingsInfoDto dto) = 10;
}
