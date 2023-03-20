package ru.sberdevices.pub.demoapp.ui.environmentinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import ru.sberdevices.messaging.Messaging
import ru.sberdevices.services.assistant.PublicAssistantLib
import ru.sberdevices.services.mic.camera.state.MicCameraStateRepository
import ru.sberdevices.services.paylib.PayLib
import ru.sberdevices.services.published.environment.info.EnvironmentInfoRepository

class EnvironmentInfoViewModel(
    private val environmentInfoRepository: EnvironmentInfoRepository,
    private val micCameraStateRepository: MicCameraStateRepository,
    private val assistantLib: PublicAssistantLib,
    private val payLib: PayLib,
    private val messaging: Messaging,
) : ViewModel() {

    val starOsVersionFlow = flow { emit(runCatching { environmentInfoRepository.getStarOsVersion() }.getOrNull()) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val deviceTypeFlow = flow { emit(runCatching { environmentInfoRepository.getDeviceType() }.getOrNull()) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val screenStateFlow = runCatching { environmentInfoRepository.screenStateFlow }
        .getOrElse { flowOf(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val userSettingsInfo = runCatching { environmentInfoRepository.userSettingsInfo }
        .getOrElse { flowOf(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val cameraStateFlow = runCatching { micCameraStateRepository.cameraState }
        .getOrElse { flowOf(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val micStateFlow = runCatching { micCameraStateRepository.micState }
        .getOrElse { flowOf(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val isCameraCovered = runCatching { micCameraStateRepository.isCameraCovered }
        .getOrElse { flowOf(null) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    val assistantServiceVersion = assistantLib.getVersion()
    val micCameraStateServiceVersion = micCameraStateRepository.getVersion()
    val environmentInfoServiceVersion = environmentInfoRepository.getVersion()
    val paylibServiceVersion = payLib.getVersion()
    val messagingServiceVersion = messaging.getVersion()
}
