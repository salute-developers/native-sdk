package ru.sberdevices.messaging

import android.os.Looper
import androidx.annotation.AnyThread
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.entities.BinderState
import ru.sberdevices.common.binderhelper.repeatOnState
import ru.sberdevices.common.binderhelper.sdk.getVersionForSdk
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.services.messaging.IMessagingListener
import ru.sberdevices.services.messaging.IMessagingService
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import ru.sberdevices.services.messaging.model.MessageName as MessageNameModel

@WorkerThread
internal class MessagingImpl @AnyThread constructor(
    private val helper: BinderHelper<IMessagingService>
) : Messaging {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logger = Logger.get("MessagingImpl")
    private val listeners = ConcurrentLinkedQueue<Messaging.Listener>()
    private val messageListener = object : IMessagingListener.Stub() {
        override fun onMessage(messageId: String?, payload: String?) {
            logger.debug { "onMessage: $messageId" }
            if (messageId != null && payload != null) {
                listeners.forEach { it.onMessage(MessageId(messageId), Payload(payload)) }
            } else {
                logger.error { "null value onMessage messageId=$messageId payload=$payload" }
            }
        }

        override fun onError(messageId: String?, error: String?) {
            logger.debug { "onError: $messageId" }
            if (messageId != null && error != null) {
                listeners.forEach { it.onError(MessageId(messageId), IOException(error)) }
            } else {
                logger.error { "null value onError messageId=$messageId error=$error" }
            }
        }

        override fun onNavigationCommand(payload: String) {
            logger.debug { "onNavigationCommand" }
            listeners.forEach { it.onNavigationCommand(Payload(payload)) }
        }
    }

    init {
        scope.repeatOnState(helper, BinderState.CONNECTED) {
            logger.debug { "registerIMessagingListener()" }
            helper.execute { it.addListener(messageListener) }
        }
        scope.repeatOnState(helper, BinderState.DISCONNECTED) {
            logger.debug { "connecting" }
            helper.connect()
        }
    }

    @SinceVersion(1)
    override fun sendAction(
        messageName: MessageName,
        payload: Payload,
        stateLevel: StateLevel,
        serverActionMode: ServerActionMode
    ): MessageId {
        logger.debug {
            "sendAction() with messageName: $messageName, stateLevel: $stateLevel, serverActionMode: $serverActionMode"
        }
        require(Looper.myLooper() != Looper.getMainLooper())

        if (stateLevel == StateLevel.UNSUPPORTED) {
            logger.warn { "Please consider setting correct StateLevel new state gathering options" }
        }
        val id: String = runBlocking {
            helper.execute {
                it.sendActionCompatible(
                    messageName = messageName,
                    payload = payload,
                    stateLevel = stateLevel,
                    serverActionMode = serverActionMode
                )
            }
        }!!
        return MessageId(id)
    }

    @SinceVersion(1)
    @RequiresPermission("ru.sberdevices.permission.CROSS_APP_ACTION")
    override fun sendAction(
        messageName: MessageName,
        payload: Payload,
        androidApplicationID: String,
        stateLevel: StateLevel,
        serverActionMode: ServerActionMode
    ): MessageId {
        logger.debug {
            "sendAction with messageName: $messageName, androidApplicationID: $androidApplicationID, " +
                "stateLevel: $stateLevel, serverActionMode: $serverActionMode"
        }
        require(Looper.myLooper() != Looper.getMainLooper())

        if (stateLevel == StateLevel.UNSUPPORTED) {
            logger.warn { "Please consider setting correct StateLevel new state gathering options" }
        }

        val id: String = runBlocking {
            helper.execute {
                it.sendActionWithAppIdCompatible(
                    messageName = messageName,
                    payload = payload,
                    androidApplicationID = androidApplicationID,
                    stateLevel = stateLevel,
                    serverActionMode = serverActionMode
                )
            }
        }!!
        return MessageId(id)
    }

    @SinceVersion(1)
    override fun sendText(text: String) {
        logger.debug { "sending text $text" }
        scope.launch { helper.execute { service -> service.sendText(text) } }
    }

    @AnyThread
    @SinceVersion(1)
    override fun addListener(listener: Messaging.Listener) {
        logger.debug { "addListener: $listener" }
        listeners.add(listener)
    }

    @AnyThread
    @SinceVersion(1)
    override fun removeListener(listener: Messaging.Listener) {
        logger.debug { "removeListener: $listener" }
        listeners.remove(listener)
    }

    override fun getVersion(): Int? {
        logger.debug { "getVersion" }
        return helper.getVersionForSdk(logger = logger)
    }

    @AnyThread
    override fun dispose() {
        logger.info { "dispose()" }
        listeners.clear()
        helper.disconnect()
        scope.cancel()
    }

    private fun IMessagingService.sendActionCompatible(
        messageName: MessageName,
        payload: Payload,
        stateLevel: StateLevel,
        serverActionMode: ServerActionMode
    ): String? {
        if (stateLevel == StateLevel.UNSUPPORTED) {
            val resultV1 = sendAction(MessageNameModel(messageName.toType()), payload.data)
            logger.debug { "sendAction v1 result: $resultV1" }
            return resultV1
        }
        val resultV3 = sendAction3(
            MessageNameModel(messageName.toType()),
            payload.data,
            stateLevel.toDto(),
            serverActionMode.toDto()
        )
        logger.debug { "sendAction v3 result: $resultV3" }
        if (resultV3 != null) return resultV3
        val resultV2 = sendAction2(
            MessageNameModel(messageName.toType()),
            payload.data,
            stateLevel.toDto()
        )
        logger.debug { "sendAction v2 result: $resultV2" }
        return resultV2
    }

    private fun IMessagingService.sendActionWithAppIdCompatible(
        messageName: MessageName,
        payload: Payload,
        androidApplicationID: String,
        stateLevel: StateLevel,
        serverActionMode: ServerActionMode
    ): String? {
        if (stateLevel == StateLevel.UNSUPPORTED) {
            val resultV1 = sendActionWithAppID(
                MessageNameModel(messageName.toType()),
                payload.data,
                androidApplicationID
            )
            logger.debug { "sendActionWithAppID v1 result: $resultV1" }
            return resultV1
        }

        val resultV3 = sendActionWithAppID3(
            MessageNameModel(type = messageName.toType()),
            payload.data,
            androidApplicationID,
            stateLevel.toDto(),
            serverActionMode.toDto()
        )

        logger.debug { "sendActionWithAppID v3 result: $resultV3" }
        if (resultV3 != null) return resultV3
        val resultV2 = sendActionWithAppID2(
            MessageNameModel(messageName.toType()), payload.data, androidApplicationID, stateLevel.toDto()
        )
        logger.debug { "sendActionWithAppID v2 result: $resultV2" }
        return resultV2
    }

    private fun StateLevel.toDto(): String = name
    private fun ServerActionMode.toDto(): String = name
    private fun MessageName.toType(): MessageNameModel.MessageNameType = when (this) {
        MessageName.SERVER_ACTION -> MessageNameModel.MessageNameType.SERVER_ACTION
        MessageName.RUN_APP -> MessageNameModel.MessageNameType.RUN_APP
        MessageName.UPDATE_IP -> MessageNameModel.MessageNameType.UPDATE_IP
        MessageName.HEARTBEAT -> MessageNameModel.MessageNameType.HEARTBEAT
        MessageName.CLOSE_APP -> MessageNameModel.MessageNameType.CLOSE_APP
        MessageName.GET_IHUB_TOKEN -> MessageNameModel.MessageNameType.GET_IHUB_TOKEN
        MessageName.RUN_APP_DEEPLINK -> MessageNameModel.MessageNameType.RUN_APP_DEEPLINK
    }
}
