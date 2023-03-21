package ru.sberdevices.services.published.environment.info.dtos.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Device lock modes supported by library.
 */
@Parcelize
enum class DeviceLockMode : Parcelable {
    NO_LOCK,
    PIN_CODE
}
