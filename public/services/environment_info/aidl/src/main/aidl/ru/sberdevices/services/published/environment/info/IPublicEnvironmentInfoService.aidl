package ru.sberdevices.services.published.environment.info;

import ru.sberdevices.services.published.environment.info.callbacks.IVersionCallback;
import ru.sberdevices.services.published.environment.info.dtos.enums.DeviceType;
import ru.sberdevices.services.published.environment.info.listeners.IScreenStateListener;
import ru.sberdevices.services.published.environment.info.listeners.IUserSettingsListener;

interface IPublicEnvironmentInfoService {
    const String PLATFORM_VERSION = "1.86.0";
    const int VERSION = 1;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    void fetchStarOsVersion(IVersionCallback callback) = 10;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    DeviceType getDeviceType() = 20;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    void registerScreenStateListener(IScreenStateListener listener) = 30;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    void unregisterScreenStateListener(IScreenStateListener listener) = 40;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    void registerUserSettingsListener(IUserSettingsListener listener) = 50;

    /** @since platform version 1.86.0 */
    /** @since version 1 */
    void unregisterUserSettingsListener(IUserSettingsListener listener) = 60;
}
