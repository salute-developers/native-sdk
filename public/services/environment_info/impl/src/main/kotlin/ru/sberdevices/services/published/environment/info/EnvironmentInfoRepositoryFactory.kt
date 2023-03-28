package ru.sberdevices.services.published.environment.info

import android.content.Context
import androidx.annotation.RequiresPermission
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory2
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.services.published.environment.info.mappers.impl.DeviceLockModeMapperImpl
import ru.sberdevices.services.published.environment.info.mappers.impl.DeviceTypeMapperImpl
import ru.sberdevices.services.published.environment.info.mappers.impl.DreamStateMapperImpl

/**
 * Фабрика [EnvironmentInfoRepository].
 * @author Максим Митюшкин on 12.01.2023
 */
class EnvironmentInfoRepositoryFactory(
    private val context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val binderHelperFactory2: BinderHelperFactory2
) {

    @RequiresPermission("ru.sberdevices.permission.BIND_PUBLIC_ENVIRONMENT_INFO_SERVICE")
    fun create(): EnvironmentInfoRepository {
        val binderHelper = createBinderHelper()

        return EnvironmentInfoRepositoryImpl(
            binderHelper = binderHelper,
            deviceTypeMapper = DeviceTypeMapperImpl(),
            dreamStateMapper = DreamStateMapperImpl(),
            deviceLockModeMapper = DeviceLockModeMapperImpl(),
            coroutineDispatchers = coroutineDispatchers,
        )
    }

    private fun createBinderHelper(): BinderHelper<IPublicEnvironmentInfoService> {
        val intent = BinderHelper.createBindIntent(packageName = SERVICE_PACKAGE_NAME, className = SERVICE_CLASS_NAME)

        return binderHelperFactory2.createCached(
            intent = intent,
            context = context,
            loggerTag = "EnvironmentInfoRepositoryImpl",
            getBinding = IPublicEnvironmentInfoService.Stub::asInterface
        )
    }

    private companion object {
        const val SERVICE_PACKAGE_NAME = "ru.sberdevices.services"
        const val SERVICE_CLASS_NAME = "ru.sberdevices.services.published.environment.info.PublicEnvironmentInfoService"
    }
}
