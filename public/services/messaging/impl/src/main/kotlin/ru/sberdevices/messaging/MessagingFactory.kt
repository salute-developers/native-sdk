package ru.sberdevices.messaging

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.AnyThread
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory
import ru.sberdevices.services.messaging.IMessagingService

object MessagingFactory {

    private const val SERVICE_APP_ID = "ru.sberdevices.services"
    private const val SERVICE_NAME = "ru.sberdevices.services.messaging.MessagingService"
    private val BIND_INTENT = Intent().apply {
        component = ComponentName(SERVICE_APP_ID, SERVICE_NAME)
    }

    @AnyThread
    @JvmStatic
    fun create(appContext: Context): Messaging {
        val binderHelper = getBinderHelper(appContext.applicationContext)
        return MessagingImpl(helper = binderHelper)
    }

    private fun getBinderHelper(context: Context): BinderHelper<IMessagingService> {
        return BinderHelperFactory(context.applicationContext, BIND_INTENT) {
            IMessagingService.Stub.asInterface(it)
        }.create()
    }
}
