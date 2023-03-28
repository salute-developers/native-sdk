package ru.sberdevices.services.appstate

import android.content.Context
import androidx.annotation.AnyThread
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.services.appstate.exceptions.AppStateManagerAlreadyExistsException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Factory for creating instances of AppStateManager. Note: it is prohibited to have more than one active (not disposed)
 * instance of AppStateManager.
 */
@AnyThread
object AppStateManagerFactory {

    private val counter = AtomicInteger()

    @JvmStatic
    @Synchronized
    @Throws(AppStateManagerAlreadyExistsException::class)
    fun createHolder(
        context: Context,
        coroutineDispatchers: CoroutineDispatchers = CoroutineDispatchers
    ): AppStateHolder {
        // counter will be incremented during creation of AppStateRequestManager inside AppStateHolder

        return AppStateHolderImpl(context.applicationContext, coroutineDispatchers)
    }

    @JvmStatic
    @Synchronized
    @Throws(AppStateManagerAlreadyExistsException::class)
    fun createRequestManager(
        context: Context,
        coroutineDispatchers: CoroutineDispatchers
    ): AppStateRequestManager {
        incrementAndCheckCounter()

        val binderHelper = getBinderHelper(context)
        return AppStateManagerImpl(
            coroutineDispatchers = coroutineDispatchers,
            helper = binderHelper,
        )
    }

    private fun getBinderHelper(context: Context) = BinderHelperFactory(
        context = context,
        getBinding = { IAppStateService.Stub.asInterface(it) },
        intent = BinderHelper.createBindIntent(
            "ru.sberdevices.services",
            "ru.sberdevices.services.appstate.AppStateService"
        ),
    ).create()

    @Synchronized
    internal fun onAppStateManagerDispose() {
        counter.decrementAndGet()
    }

    private fun incrementAndCheckCounter() {
        if (counter.incrementAndGet() > 1) {
            throw AppStateManagerAlreadyExistsException()
        }
    }
}
