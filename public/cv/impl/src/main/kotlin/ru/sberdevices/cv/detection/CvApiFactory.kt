package ru.sberdevices.cv.detection

import android.content.Context
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.BinderHelperFactory2
import ru.sberdevices.common.binderhelper.BinderHelperFactory2Impl
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.cv.ICvDetectionService
import ru.sberdevices.cv.util.BindingIdStorage
import ru.sberdevices.cv.util.BindingIdStorageImpl

private const val SERVICE_COMPONENT_PACKAGE = "ru.sberdevices.cv"
private const val SERVICE_COMPONENT_CLASS = "CvDetectionService"
private const val LOG_TAG = "CvDetectionApiBinding"
private val serviceIntent = BinderHelper.createBindIntent(
    packageName = SERVICE_COMPONENT_PACKAGE,
    className = "$SERVICE_COMPONENT_PACKAGE.$SERVICE_COMPONENT_CLASS"
)

interface CvApiFactory {
    fun get(): CvApi
}

class CvApiFactoryImpl(
    private val context: Context,
    private val bindingIdStorage: BindingIdStorage = BindingIdStorageImpl(),
    private val binderHelperFactory2: BinderHelperFactory2 = BinderHelperFactory2Impl(),
    private val coroutineDispatchers: CoroutineDispatchers = CoroutineDispatchers.Default,
    private val deathListenerFactory: DeathListenerFactory = DeathListenerFactoryImpl(),
    private val humansDetectionListenerFactory: HumansDetectionListenerFactory = HumansDetectionListenerFactoryImpl(),
    private val gestureDetectionListenerFactory: GestureDetectionListenerFactory =
        GestureDetectionListenerFactoryImpl(),
    private val mirrorDetectedListenerFactory: MirrorDetectedListenerFactory = MirrorDetectedListenerFactoryImpl()
) : CvApiFactory {

    constructor(context: Context) : this(
        context = context,
        bindingIdStorage = BindingIdStorageImpl(),
        binderHelperFactory2 = BinderHelperFactory2Impl(),
        coroutineDispatchers = CoroutineDispatchers.Default,
        deathListenerFactory = DeathListenerFactoryImpl(),
        humansDetectionListenerFactory = HumansDetectionListenerFactoryImpl(),
        gestureDetectionListenerFactory = GestureDetectionListenerFactoryImpl(),
        mirrorDetectedListenerFactory = MirrorDetectedListenerFactoryImpl()
    )

    constructor(context: Context, bindingIdStorage: BindingIdStorage) : this(
        context = context,
        bindingIdStorage = bindingIdStorage,
        binderHelperFactory2 = BinderHelperFactory2Impl(),
        coroutineDispatchers = CoroutineDispatchers.Default,
        deathListenerFactory = DeathListenerFactoryImpl(),
        humansDetectionListenerFactory = HumansDetectionListenerFactoryImpl(),
        gestureDetectionListenerFactory = GestureDetectionListenerFactoryImpl(),
        mirrorDetectedListenerFactory = MirrorDetectedListenerFactoryImpl()
    )

    override fun get(): CvApi {
        val binderHelper = binderHelperFactory2.create(context, serviceIntent, LOG_TAG) {
            ICvDetectionService.Stub.asInterface(it)
        }
        return CvDetectionApiBinding(
            bindingIdStorage = bindingIdStorage,
            binderHelper = binderHelper,
            coroutineDispatchers = coroutineDispatchers,
            deathListenerFactory = deathListenerFactory,
            humansDetectionListenerFactory = humansDetectionListenerFactory,
            gestureDetectionListenerFactory = gestureDetectionListenerFactory,
            mirrorDetectedListenerFactory = mirrorDetectedListenerFactory
        )
    }
}
