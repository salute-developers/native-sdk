package ru.sberdevices.services.mic.camera.state

import android.content.Context
import androidx.annotation.RequiresPermission
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory
import ru.sberdevices.common.binderhelper.CachedBinderHelper
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.services.mic.camera.state.aidl.IMicCameraStateService
import ru.sberdevices.services.mic.camera.state.aidl.wrappers.OnMicCameraStateChangedListenerWrapperImpl

/**
 * Фактори для [MicCameraStateRepository].
 */
class MicCameraStateRepositoryFactory constructor(
    context: Context,
    private val coroutineDispatchers: CoroutineDispatchers
) {

    private val applicationContext = context.applicationContext

    @RequiresPermission("ru.sberdevices.permission.BIND_MIC_CAMERA_STATE_SERVICE")
    fun create(): MicCameraStateRepository {
        val binderHelper = getBinderHelper(applicationContext)

        return MicCameraStateRepositoryImpl(
            helper = binderHelper,
            coroutineDispatchers = coroutineDispatchers,
            onMicCameraStateChangedListenerWrapper = OnMicCameraStateChangedListenerWrapperImpl()
        )
    }

    private fun getBinderHelper(context: Context): CachedBinderHelper<IMicCameraStateService> {
        val bindIntent = BinderHelper.createBindIntent(
            packageName = "ru.sberdevices.services",
            className = "ru.sberdevices.services.mic.camera.state.MicCameraStateService"
        )

        return BinderHelperFactory(context, bindIntent, Logger.get("MicCameraStateRepositoryImpl")) {
            IMicCameraStateService.Stub.asInterface(it)
        }.createCached()
    }
}
