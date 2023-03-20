package ru.sberdevices.services.published.environment.info.dtos.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Device dream modes supported by library.
 */
@Parcelize
enum class DreamState : Parcelable {
    SCREENSAVER,
    NIGHT,
    AWAKE
}
