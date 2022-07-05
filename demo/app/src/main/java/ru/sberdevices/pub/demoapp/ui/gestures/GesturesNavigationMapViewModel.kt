package ru.sberdevices.sdk.demoapp.ui.gestures

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.messaging.MessageId
import ru.sberdevices.messaging.Messaging
import ru.sberdevices.messaging.Payload
import ru.sberdevices.pub.demoapp.repository.Gesture
import ru.sberdevices.pub.demoapp.repository.GesturesRepository
import ru.sberdevices.pub.demoapp.ui.smartapp.model.NavCommand
import ru.sberdevices.pub.demoapp.ui.smartapp.model.NavigationCommand
import ru.sberdevices.sdk.demoapp.ui.gestures.controller.GridController

internal class GesturesNavigationMapViewModel(
    val gridController: GridController,
    gesturesRepository: GesturesRepository,
    val messaging: Messaging
) : ViewModel() {

    private val commandParser = Json {
        classDiscriminator = "command"
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val logger = Logger.get("GesturesNavigationMapViewModel")

    private val navFlow: MutableSharedFlow<Gesture> = MutableSharedFlow()

    private val listener = object : Messaging.Listener {
        override fun onError(messageId: MessageId, throwable: Throwable) = Unit

        override fun onMessage(messageId: MessageId, payload: Payload) = Unit

        override fun onNavigationCommand(payload: Payload) {
            val navCommand = commandParser.decodeFromString<NavigationCommand>(payload.data)

            when (navCommand.command) {
                NavCommand.UP -> navFlow.tryEmit(Gesture.SWIPE_UP)
                NavCommand.DOWN -> navFlow.tryEmit(Gesture.SWIPE_DOWN)
                NavCommand.LEFT -> navFlow.tryEmit(Gesture.SWIPE_LEFT)
                NavCommand.RIGHT -> navFlow.tryEmit(Gesture.SWIPE_RIGHT)
                NavCommand.FORWARD -> navFlow.tryEmit(Gesture.SWIPE_RIGHT)
            }
        }
    }

    val gesturesFlow = merge(gesturesRepository.gestureNavigationFlow, navFlow)

    init {
        logger.debug { "init" }
        messaging.addListener(listener)
    }
}
