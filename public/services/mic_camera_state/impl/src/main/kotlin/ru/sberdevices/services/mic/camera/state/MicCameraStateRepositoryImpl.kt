package ru.sberdevices.services.mic.camera.state

import androidx.annotation.AnyThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.entities.BinderState
import ru.sberdevices.common.binderhelper.sdk.getVersionForSdk
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.services.mic.camera.state.aidl.IMicCameraStateService
import ru.sberdevices.services.mic.camera.state.aidl.wrappers.OnMicCameraStateChangedListenerWrapper

/**
 * Имплементация [MicCameraStateRepository].
 */
internal class MicCameraStateRepositoryImpl(
    private val helper: BinderHelper<IMicCameraStateService>,
    coroutineDispatchers: CoroutineDispatchers,
    private val onMicCameraStateChangedListenerWrapper: OnMicCameraStateChangedListenerWrapper
) : MicCameraStateRepository {

    private val logger by lazy { Logger.get("MicCameraStateRepositoryImpl") }
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineDispatchers.io)

    @SinceVersion(1)
    override val micState: Flow<MicCameraStateRepository.State> = onMicCameraStateChangedListenerWrapper.micStateFlow
        .onEach { logger.debug { "MicState changed to $it" } }
        .flowOn(coroutineDispatchers.default)

    @SinceVersion(1)
    override val cameraState: Flow<MicCameraStateRepository.State> =
        onMicCameraStateChangedListenerWrapper.cameraStateFlow
            .onEach { logger.debug { "CameraState changed to $it" } }
            .flowOn(coroutineDispatchers.default)

    @SinceVersion(1)
    override val isCameraCovered: Flow<Boolean> = onMicCameraStateChangedListenerWrapper.isCameraCoveredStateFlow
        .onEach { logger.debug { "IsCameraCovered changed to $it" } }
        .flowOn(coroutineDispatchers.default)

    init {
        helper.binderStateFlow
            .filter { it == BinderState.CONNECTED }
            .onEach {
                logger.debug { "connected" }
                helper.execute { it.registerMicCameraStateListener(onMicCameraStateChangedListenerWrapper) }
            }
            .launchIn(coroutineScope)
        helper.connect()
    }

    @AnyThread
    @SinceVersion(1)
    override fun setCameraEnabled(newState: Boolean) {
        logger.debug { "setCameraEnabled: $newState" }
        coroutineScope.launch {
            helper.execute { service -> service.setCameraEnabled(newState) }
        }
    }

    @AnyThread
    @SinceVersion(1)
    override fun setMicEnabled(newState: Boolean) {
        logger.debug { "setMicEnabled: $newState" }
        coroutineScope.launch {
            helper.execute { service -> service.setMicEnabled(newState) }
        }
    }

    override fun getVersion(): Int? {
        logger.debug { "getVersion" }
        return helper.getVersionForSdk(logger = logger)
    }

    @Synchronized
    override fun dispose() {
        logger.debug { "dispose()" }
        coroutineScope.launch {
            logger.debug { "unregisterMicCameraStateListener()" }
            helper.execute { it.unregisterMicCameraStateListener(onMicCameraStateChangedListenerWrapper) }
            helper.disconnect()
            coroutineScope.cancel()
        }
    }
}
