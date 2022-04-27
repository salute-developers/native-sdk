package ru.sberdevices.services.assistant;

interface IPublicAssistantService {
    const String PLATFORM_VERSION = "1.80.0";

    /** @since platform version 1.80.0 */
    void cancelAssistantSpeech(in String appInfo) = 10;
}
