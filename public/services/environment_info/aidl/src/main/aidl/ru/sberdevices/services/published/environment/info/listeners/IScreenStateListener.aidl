package ru.sberdevices.services.published.environment.info.listeners;

import ru.sberdevices.services.published.environment.info.dtos.ScreenStateDto;

interface IScreenStateListener {
    oneway void onScreenStateChange(in ScreenStateDto dto) = 10;
}
