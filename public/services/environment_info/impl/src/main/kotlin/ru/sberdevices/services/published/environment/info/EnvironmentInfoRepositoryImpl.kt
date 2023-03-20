package ru.sberdevices.services.published.environment.info

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.entities.BinderState
import ru.sberdevices.common.binderhelper.repeatOnState
import ru.sberdevices.common.binderhelper.sdk.getVersionForSdk
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.services.published.environment.info.callbacks.IVersionCallback
import ru.sberdevices.services.published.environment.info.dtos.ScreenStateDto
import ru.sberdevices.services.published.environment.info.dtos.UserSettingsInfoDto
import ru.sberdevices.services.published.environment.info.listeners.IScreenStateListener
import ru.sberdevices.services.published.environment.info.listeners.IUserSettingsListener
import ru.sberdevices.services.published.environment.info.mappers.DeviceLockModeMapper
import ru.sberdevices.services.published.environment.info.mappers.DeviceTypeMapper
import ru.sberdevices.services.published.environment.info.mappers.DreamStateMapper
import ru.sberdevices.services.published.environment.info.models.ScreenState
import ru.sberdevices.services.published.environment.info.models.UserSettingsInfo
import ru.sberdevices.services.published.environment.info.models.enums.DeviceType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Реализация [EnvironmentInfoRepository].
 * @author Максим Митюшкин on 12.01.2023
 */
internal class EnvironmentInfoRepositoryImpl(
    private val binderHelper: BinderHelper<IPublicEnvironmentInfoService>,
    private val deviceTypeMapper: DeviceTypeMapper,
    private val dreamStateMapper: DreamStateMapper,
    private val deviceLockModeMapper: DeviceLockModeMapper,
    coroutineDispatchers: CoroutineDispatchers,
) : EnvironmentInfoRepository {

    private val scope = CoroutineScope(coroutineDispatchers.default + SupervisorJob())
    private val logger = Logger.get("EnvironmentInfoRepositoryImpl")

    @SinceVersion(1)
    override val screenStateFlow: Flow<ScreenState> = callbackFlow {
        logger.debug { "screenStateFlow connecting to service" }
        binderHelper.connect()

        val listener = object : IScreenStateListener.Stub() {
            override fun onScreenStateChange(dto: ScreenStateDto) {
                logger.debug { "onScreenStateChange()" }
                trySend(dto)
            }
        }

        repeatOnState(binderHelper, BinderState.CONNECTED) {
            logger.debug { "registering ScreenStateListener" }
            binderHelper.execute { it.registerScreenStateListener(listener) }
        }

        awaitClose {
            logger.debug { "awaitClose, removing ScreenStateListener" }
            binderHelper.tryExecute { it.unregisterScreenStateListener(listener) }
            binderHelper.disconnect()
        }
    }.map { dto ->
        ScreenState(
            isNoScreenModeEnabled = dto.isNoScreenModeEnabled,
            dreamState = dreamStateMapper.map(dto.dreamState)
        )
    }.shareIn(scope, started = SharingStarted.WhileSubscribed(), replay = 1)

    @SinceVersion(1)
    override val userSettingsInfo: Flow<UserSettingsInfo> = callbackFlow {
        logger.debug { "userSettingsInfo connecting to service" }
        binderHelper.connect()

        val listener = object : IUserSettingsListener.Stub() {
            override fun onUserSettingsChange(dto: UserSettingsInfoDto) {
                logger.debug { "onUserSettingsChange()" }
                trySend(dto)
            }
        }

        repeatOnState(binderHelper, BinderState.CONNECTED) {
            logger.debug { "registering UserSettingsListener" }
            binderHelper.execute { it.registerUserSettingsListener(listener) }
        }

        awaitClose {
            logger.debug { "awaitClose, removing UserSettingsListener" }
            binderHelper.tryExecute { it.unregisterUserSettingsListener(listener) }
            binderHelper.disconnect()
        }
    }.map { dto ->
        UserSettingsInfo(
            isChildModeEnabled = dto.isChildModeEnabled,
            deviceLockMode = deviceLockModeMapper.map(dto.deviceLockMode)
        )
    }.shareIn(scope, started = SharingStarted.WhileSubscribed(), replay = 1)

    @SinceVersion(1)
    override suspend fun getStarOsVersion(): String {
        val result = binderHelper.suspendExecute { service ->
            suspendCoroutine<String> { continuation ->
                service.fetchStarOsVersion(object : IVersionCallback.Stub() {
                    override fun onVersionResult(version: String) {
                        continuation.resume(version)
                    }
                })
            }
        }

        if (result == null) {
            logger.warn { "getStarOsVersion(): result = null" }
            return "unknown"
        }

        return result
    }

    @SinceVersion(1)
    override suspend fun getDeviceType(): DeviceType {
        val result = binderHelper.suspendExecute { it.deviceType }
        if (result == null) {
            logger.warn { "getDeviceType(): result = null" }
            return DeviceType.UNKNOWN
        }

        return deviceTypeMapper.map(result)
    }

    override fun getVersion(): Int? {
        logger.debug { "getVersion" }
        return binderHelper.getVersionForSdk(logger = logger)
    }
}
