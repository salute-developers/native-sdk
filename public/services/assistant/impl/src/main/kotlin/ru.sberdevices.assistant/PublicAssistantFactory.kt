package ru.sberdevices.services.assistant

import android.content.Context
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory2
import ru.sberdevices.common.binderhelper.CachedBinderHelper
import ru.sberdevices.common.binderhelper.createCached
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.services.assistant.IPublicAssistantService

/**
 * Factory for creating [PublicAssistantLib]
 */
class PublicAssistantFactory(
    private val context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val binderHelperFactory2: BinderHelperFactory2
) {

    fun create(): PublicAssistantLib {
        val binderHelper = getBinderHelper()

        return PublicAssistantLibImpl(
            binderHelper = binderHelper,
            coroutineDispatchers = coroutineDispatchers
        )
    }

    private fun getBinderHelper(): CachedBinderHelper<IPublicAssistantService> {
        val bindIntent = BinderHelper.createBindIntent(
            packageName = "ru.sberdevices.services",
            className = "ru.sberdevices.services.assistant.PublicAssistantService"
        )

        return binderHelperFactory2.createCached(context, bindIntent) {
            IPublicAssistantService.Stub.asInterface(it)
        }
    }
}
