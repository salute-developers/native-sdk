package ru.sberdevices.services.published.environment.info

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Объект-хелпер для быстрого получения данных об окружении.
 * @author Махинов Сергей on 22.01.24
 */
object EnvironmentInfoHelper {

    @Volatile
    private var isSberDevice: Boolean? = null

    /**
     * Определяет, что текущее устройство выпущено SberDevices.
     * @param context Текущий контекст приложения.
     * @return true, если устройство выпущено SberDevices, иначе false.
     */
    fun isSberDevice(context: Context): Boolean {
        Log.d(LOG_TAG, "isSberDevice()")

        if (isSberDevice == null) {
            synchronized(this) {
                if (isSberDevice == null) {
                    isSberDevice = runCatching {
                        context
                            .packageManager
                            .getPackageInfo(SERVICES_PACKAGE_NAME, PackageManager.GET_META_DATA)
                    }.fold(onSuccess = {
                        Log.d(LOG_TAG, "isSberDevice(): system services found!")
                        true
                    }, onFailure = {
                        Log.d(LOG_TAG, "isSberDevice(): system services not found!", it)
                        false
                    })
                }
            }
        }

        return requireNotNull(isSberDevice)
    }

    private const val SERVICES_PACKAGE_NAME = "ru.sberdevices.services"
    private const val LOG_TAG = "EnvironmentInfoHelper"
}
