package ru.sberdevices.services.paylib

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory2
import ru.sberdevices.common.binderhelper.CachedBinderHelper
import ru.sberdevices.common.binderhelper.createCached
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.services.paylib.aidl.wrappers.PayStatusListenerWrapperImpl

/**
 * Фабрика для создания экземпляра [PayLib]
 */
class PayLibFactory(
    private val context: Context,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val binderHelperFactory2: BinderHelperFactory2,
) {

    /**
     * @return Экземпляр, работающий по биндеру.
     */
    fun create(): PayLib {
        val binderHelper = getBinderHelper()

        return PayLibImpl(
            helper = binderHelper,
            dispatchers = coroutineDispatchers,
            payStatusListenerWrapper = PayStatusListenerWrapperImpl(),
            callbackScope = CoroutineScope(SupervisorJob() + coroutineDispatchers.default)
        )
    }

    private fun getBinderHelper(): CachedBinderHelper<IPayLibService> {
        val bindIntent = BinderHelper.createBindIntent(
            packageName = "ru.sberdevices.services",
            className = "ru.sberdevices.services.pay.PayLibService"
        )

        return binderHelperFactory2.createCached(context, bindIntent) {
            IPayLibService.Stub.asInterface(it)
        }
    }
}
