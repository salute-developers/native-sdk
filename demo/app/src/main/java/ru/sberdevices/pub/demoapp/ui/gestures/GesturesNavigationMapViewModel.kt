package ru.sberdevices.sdk.demoapp.ui.gestures

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.SharedFlow
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.pub.demoapp.repository.Gesture
import ru.sberdevices.pub.demoapp.repository.GesturesRepository
import ru.sberdevices.sdk.demoapp.ui.gestures.controller.GridController

internal class GesturesNavigationMapViewModel(
    val gridController: GridController,
    gesturesRepository: GesturesRepository
) : ViewModel() {

    private val logger = Logger.get("GesturesNavigationMapViewModel")

    init {
        logger.debug { "init" }
    }

    val gesturesFlow: SharedFlow<Gesture> = gesturesRepository.gestureNavigationFlow
}
