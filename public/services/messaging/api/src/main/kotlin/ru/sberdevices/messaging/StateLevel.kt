package ru.sberdevices.messaging

/**
 * Level of System state gathering.
 * @author Nikolay Pakhomov on 22.04.2022
 */
enum class StateLevel {
    /**
     * System state gathered by StarOS will include applications.
     */
    ALL,

    /**
     * System state gathered by StarOS will exclude applications' state.
     */
    WITHOUT_APPS,

    @Deprecated("Default value for older clients interoperability. Not for manual use")
    UNSUPPORTED,
}
