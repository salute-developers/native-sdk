package ru.sberdevices.services.assistant;

interface IPublicAssistantService {
    const String PLATFORM_VERSION = "1.80.0";
    const int VERSION = 1;

    /** @since platform version 1.80.0 */
    /** @since version 1 */
    void cancelAssistantSpeech(in String appInfo) = 10;
}
