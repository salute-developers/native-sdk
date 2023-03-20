package ru.sberdevices.services.assistant

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.sberdevices.common.binderhelper.CachedBinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.sdk.getVersionForSdk
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.services.assistant.IPublicAssistantService

internal class PublicAssistantLibImpl(
    private val binderHelper: CachedBinderHelper<IPublicAssistantService>,
    coroutineDispatchers: CoroutineDispatchers
) : PublicAssistantLib {

    private val logger = Logger.get("PublicAssistantLibImpl")
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineDispatchers.io)

    @SinceVersion(1)
    override fun cancelAssistantSpeech(appInfo: String) {
        logger.debug { "cancelAssistantSpeech()" }

        coroutineScope.launch {
            binderHelper.execute { service ->
                service.cancelAssistantSpeech(appInfo)
            }
        }
    }

    override fun getVersion(): Int? {
        logger.debug { "getVersion" }
        return binderHelper.getVersionForSdk(logger = logger)
    }

    @Synchronized
    override fun dispose() {
        logger.info { "dispose()" }
        binderHelper.disconnect()
        coroutineScope.cancel()
    }
}
