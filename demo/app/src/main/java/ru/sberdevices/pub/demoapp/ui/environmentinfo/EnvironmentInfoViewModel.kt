package ru.sberdevices.pub.demoapp.ui.environmentinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import ru.sberdevices.messaging.Messaging
import ru.sberdevices.services.assistant.PublicAssistantLib
import ru.sberdevices.services.mic.camera.state.MicCameraStateRepository
import ru.sberdevices.services.paylib.PayLib
import ru.sberdevices.services.published.environment.info.EnvironmentInfoRepository
import ru.sberdevices.services.published.environment.info.models.ScreenState
import ru.sberdevices.services.published.environment.info.models.UserSettingsInfo

class EnvironmentInfoViewModel(
    private val environmentInfoRepository: EnvironmentInfoRepository,
    private val micCameraStateRepository: MicCameraStateRepository,
    assistantLib: PublicAssistantLib,
    payLib: PayLib,
    messaging: Messaging,
) : ViewModel() {

    val starOsVersionFlow = flow {
        emit(
            checkVersionAndInvoke(
                getServiceVersion = environmentInfoRepository::getVersion,
                minServiceVersion = 1,
                valueIfIncompatible = null,
                getValueIfCompatible = environmentInfoRepository::getStarOsVersion
            )
        )
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val deviceTypeFlow = flow {
        emit(
            checkVersionAndInvoke(
                getServiceVersion = environmentInfoRepository::getVersion,
                minServiceVersion = 1,
                valueIfIncompatible = null,
                getValueIfCompatible = environmentInfoRepository::getDeviceType
            )
        )
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val screenStateFlow = flow {
        emitAll(checkVersionAndInvoke(
            getServiceVersion = environmentInfoRepository::getVersion,
            minServiceVersion = 1,
            valueIfIncompatible = flowOf<ScreenState?>(null),
            getValueIfCompatible = { environmentInfoRepository.screenStateFlow }
        ))
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val userSettingsInfo = flow {
        emitAll(checkVersionAndInvoke(
            getServiceVersion = environmentInfoRepository::getVersion,
            minServiceVersion = 1,
            valueIfIncompatible = flowOf<UserSettingsInfo?>(null),
            getValueIfCompatible = { environmentInfoRepository.userSettingsInfo }
        ))
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val cameraStateFlow = flow {
        emitAll(checkVersionAndInvoke(
            getServiceVersion = micCameraStateRepository::getVersion,
            minServiceVersion = 1,
            valueIfIncompatible = flowOf<MicCameraStateRepository.State?>(null),
            getValueIfCompatible = { micCameraStateRepository.cameraState }
        ))
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val micStateFlow = flow {
        emitAll(checkVersionAndInvoke(
            getServiceVersion = micCameraStateRepository::getVersion,
            minServiceVersion = 1,
            valueIfIncompatible = flowOf<MicCameraStateRepository.State?>(null),
            getValueIfCompatible = { micCameraStateRepository.micState }
        ))
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val isCameraCovered = flow {
        emitAll(checkVersionAndInvoke(
            getServiceVersion = micCameraStateRepository::getVersion,
            minServiceVersion = 1,
            valueIfIncompatible = flowOf<Boolean?>(null),
            getValueIfCompatible = { micCameraStateRepository.isCameraCovered }
        ))
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val assistantServiceVersion = assistantLib.getVersion()
    val micCameraStateServiceVersion = micCameraStateRepository.getVersion()
    val environmentInfoServiceVersion = environmentInfoRepository.getVersion()
    val paylibServiceVersion = payLib.getVersion()
    val messagingServiceVersion = messaging.getVersion()

    private suspend fun <T> checkVersionAndInvoke(
        getServiceVersion: () -> Int?,
        minServiceVersion: Int,
        valueIfIncompatible: T,
        getValueIfCompatible: suspend () -> T
    ): T {
        val version = getServiceVersion() ?: return valueIfIncompatible
        return if (version >= minServiceVersion) {
            getValueIfCompatible()
        } else {
            valueIfIncompatible
        }
    }
}
