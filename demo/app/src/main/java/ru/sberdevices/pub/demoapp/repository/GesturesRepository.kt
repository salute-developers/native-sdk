package ru.sberdevices.pub.demoapp.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.sberdevices.common.logger.Logger

class GesturesRepository {

    private val logger = Logger.get("GesturesRepository")

    private val _gestureNavigationEnabledFlow = MutableStateFlow(true)
    private val _gestureNavigationFlow = MutableSharedFlow<Gesture>(extraBufferCapacity = 1)

    val gestureNavigationEnabledFlow: StateFlow<Boolean> = _gestureNavigationEnabledFlow
    val gestureNavigationFlow: SharedFlow<Gesture> = _gestureNavigationFlow

    fun toggleGestureNavigation() {
        logger.debug { "toggleGestureNavigation" }
        _gestureNavigationEnabledFlow.value = !_gestureNavigationEnabledFlow.value
    }

    fun onGestureDetected(gesture: Gesture) {
        logger.debug { "onGestureDetected: $gesture" }
        _gestureNavigationFlow.tryEmit(gesture)
    }
}

enum class Gesture {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT
}
