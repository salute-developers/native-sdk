package ru.sberdevices.services.published.environment.info.dtos.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Type of devices supported by library.
 */
@Parcelize
enum class DeviceType : Parcelable {
    STARGATE,
    SBERBOX,
    SMARTBOX,
    SBERBOX_TOP,
    TV,
    HEAD_UNIT,
    UNKNOWN,
    BOOM,
    BOOM_MINI,
    SBER_TIME
}
