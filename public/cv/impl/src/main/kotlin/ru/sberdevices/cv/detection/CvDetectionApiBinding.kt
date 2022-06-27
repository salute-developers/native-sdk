@file:Suppress("unused", "WeakerAccess")

package ru.sberdevices.cv.detection

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.entities.BinderState
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.cv.ICvDetectionService
import ru.sberdevices.cv.ServiceInfo
import ru.sberdevices.cv.api.BuildConfig
import ru.sberdevices.cv.detection.entity.IGestureDetectionListener
import ru.sberdevices.cv.detection.entity.IHumansDetectionListener
import ru.sberdevices.cv.detection.entity.IMirrorDetectedListener
import ru.sberdevices.cv.detection.entity.gesture.Gesture
import ru.sberdevices.cv.detection.entity.humans.Humans
import ru.sberdevices.cv.detection.entity.humans.HumansDetectionAspect
import ru.sberdevices.cv.detection.util.toGesture
import ru.sberdevices.cv.detection.util.toHumans
import ru.sberdevices.cv.util.BindingIdStorage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val BINDING_CLOSING_DELAY_MS = 10L

internal class CvDetectionApiBinding(
    private val bindingIdStorage: BindingIdStorage,
    private val binderHelper: BinderHelper<ICvDetectionService>,
    private val deathListenerFactory: DeathListenerFactory,
    private val humansDetectionListenerFactory: HumansDetectionListenerFactory,
    private val gestureDetectionListenerFactory: GestureDetectionListenerFactory,
    private val mirrorDetectedListenerFactory: MirrorDetectedListenerFactory,
    coroutineDispatchers: CoroutineDispatchers
) : CvApi {
    private val logger = Logger.get("CvDetectionApiBinding")

    private val bindingId = bindingIdStorage.bindingId

    private val coroutineScope =
        CoroutineScope(
            SupervisorJob() + coroutineDispatchers.io + CoroutineExceptionHandler { coroutineContext, throwable ->
                logger.error(throwable) { "coroutine $coroutineContext exception" }
            }
        )

    private val humansSubscriptions = ConcurrentHashMap<String, Set<HumansDetectionAspect>>()

    private val humanRecognitionsAspects get() = humansSubscriptions.values.flatten().toSet()

    private val callbacksCounter = AtomicInteger(3) // По количеству доступных детекций

    @Volatile
    private var humansListener: IHumansDetectionListener? = null

    @Volatile
    private var lastHumans: Humans = Humans.EMPTY

    private val humansCallbackFlow = callbackFlow<ByteArray> {
        logger.verbose { "create humans callback flow $this" }
        withBindingId { id, service ->
            val aspects = humanRecognitionsAspects
            logger.debug { "subscribe $this for humans detection with aspects $aspects" }
            humansListener = humansDetectionListenerFactory.getListener { detectionEntity ->
                trySend(detectionEntity)
            }
            service.subscribeForHumansDetection(
                id,
                humansListener,
                aspects.map { it.code }.toByteArray()
            )
        }
        awaitClose {
            withBindingId { id, service ->
                logger.debug { "unsubscribe $this from humans detection" }
                service.unsubscribeFromHumansDetection(id, humansListener)
                humansListener = null
            }
        }
    }

    private val humansSharedFlow = humansCallbackFlow.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )
    private val humansMutex = Mutex()

    @Volatile
    private var cropMode: Boolean = false

    @Volatile
    private var trackIdsIn: IntArray? = null

    @Volatile
    private var logsEnable: Boolean = false

    @Volatile
    private var gesturesListener: IGestureDetectionListener? = null
    private val gestureCallbackFlow = callbackFlow<Gesture> {
        logger.verbose { "create gestures callback flow $this" }
        withBindingId { id, service ->
            gesturesListener = gestureDetectionListenerFactory.getListener { detectionEntity ->
                val detection = detectionEntity.toGesture()
                if (detection != null) {
                    logger.debug { "detected gesture ${detection.type}" }
                    trySend(detection)
                }
            }
            logger.debug { "subscribe $this for gestures detection" }
            service.subscribeForGestureDetection(id, gesturesListener)
        }

        awaitClose {
            withBindingId { id, service ->
                logger.debug { "unsubscribe $this from gestures detection" }
                service.unsubscribeFromGestureDetection(id, gesturesListener)
                gesturesListener = null
            }
        }
    }
    private val gestureSharedFlow = gestureCallbackFlow.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )

    @Volatile
    private var isMirrorDetectedListener: IMirrorDetectedListener? = null
    private val isMirrorDetectedCallbackFlow = callbackFlow {
        logger.verbose { "create is mirror detected callback flow $this" }
        withBindingId { id, service ->
            isMirrorDetectedListener = mirrorDetectedListenerFactory.getListener { detected -> trySend(detected) }
            logger.debug { "subscribe $this for mirror detection" }
            service.subscribeForIsMirrorDetected(id, isMirrorDetectedListener)
        }
        awaitClose {
            withBindingId { id, service ->
                logger.debug { "unsubscribe $this from mirror detection" }
                service.unsubscribeFromIsMirrorDetected(id, isMirrorDetectedListener)
                isMirrorDetectedListener = null
            }
        }
    }
    private val isMirrorDetectedSharedFlow = isMirrorDetectedCallbackFlow.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 0
    )

    init {
        if (isAvailableOnDevice()) {
            subscribeToServiceConnectionLifecycleEvents()
            binderHelper.connect()
            generateBindingId()
            setupDeathListener()
        }
    }

    override fun isAvailableOnDevice(): Boolean {
        val isAvailable = binderHelper.hasService()
        logger.debug { "available on device: $isAvailable" }
        return isAvailable
    }

    private fun generateBindingId() {
        coroutineScope.launch {
            binderHelper.execute { service ->
                val storedBindingId = bindingIdStorage.get()
                if (storedBindingId == null) {
                    val generatedBindingId = service.bindingId
                    if (generatedBindingId != null) {
                        bindingIdStorage.set(generatedBindingId)
                    }
                }
            }
        }
    }

    private fun setupDeathListener() {
        withBindingId { id, service ->
            service.sendDeathListener(
                id,
                deathListenerFactory.getListener()
            )
            val version = getBindingCvApiVersion()
            logger.verbose { "sending clients cv api version to service: $version" }
            service.sendClientCvApiVersion(id, version)
        }
    }

    private fun subscribeToServiceConnectionLifecycleEvents() {
        binderHelper.binderStateFlow.onEach { event ->
            when (event) {
                BinderState.CONNECTED -> restoreDetection()
                BinderState.DISCONNECTED,
                BinderState.BINDING_DIED,
                BinderState.NULL_BINDING -> {
                    /** Do nothing */
                }
            }
        }.launchIn(coroutineScope)
    }

    private fun restoreDetection() {
        logger.debug { "Connected to cv detection service" }
        coroutineScope.launch {
            val shouldRestoreHumansDetection = humansListener != null &&
                    humanRecognitionsAspects.isNotEmpty()
            if (shouldRestoreHumansDetection) {
                withBindingId { id, service ->
                    logger.debug {
                        "restoring $humansListener detections for humans with aspects $humanRecognitionsAspects " +
                                "for binding id ${bindingId.value}"
                    }
                    service.subscribeForHumansDetection(
                        id,
                        humansListener,
                        humanRecognitionsAspects.map { it.code }.toByteArray()
                    )
                }
            }
        }
        coroutineScope.launch {
            if (gesturesListener != null) {
                withBindingId { id, service ->
                    logger.debug {
                        "restoring $gesturesListener detections for gestures for " +
                                "binding id ${bindingId.value}"
                    }
                    service.subscribeForGestureDetection(
                        id,
                        gesturesListener
                    )
                }
            }
        }
        coroutineScope.launch {
            if (isMirrorDetectedListener != null) {
                withBindingId { id, service ->
                    logger.debug {
                        "restoring $isMirrorDetectedListener detections for mirror for " +
                                "binding id ${bindingId.value}"
                    }
                    service.subscribeForIsMirrorDetected(
                        id,
                        isMirrorDetectedListener
                    )
                }
            }
        }
    }

    override fun close() {
        withBindingId { id, service ->
            logger.debug { "stop observing humans with aspects $humanRecognitionsAspects" }
            humansSubscriptions.clear()
            service.unsubscribeFromHumansDetection(id, humansListener)
            humansListener = null
            callbacksCounter.getAndDecrement()
        }

        withBindingId { id, service ->
            logger.debug { "stop observing gestures" }
            service.unsubscribeFromGestureDetection(id, gesturesListener)
            gesturesListener = null
            callbacksCounter.getAndDecrement()
        }

        withBindingId { id, service ->
            logger.debug { "stop observing mirror" }
            service.unsubscribeFromIsMirrorDetected(id, isMirrorDetectedListener)
            isMirrorDetectedListener = null
            callbacksCounter.getAndDecrement()
        }

        coroutineScope.launch {
            while (callbacksCounter.get() > 0) {
                logger.debug { "trying close binding, but some active callbacks yet present" }
                delay(BINDING_CLOSING_DELAY_MS)
            }
            binderHelper.disconnect()
            coroutineScope.cancel()
            logger.info { "binding closed" }
        }
    }

    override suspend fun setPoseCropMode(cropMode: Boolean) {
        withBindingId { id, service ->
            this.cropMode = cropMode
            logger.info { "cv CvDetectionApiBinding.kt cropMode=$cropMode" }
            service.setPoseCropMode(id, cropMode)
        }
    }

    override suspend fun setPoseActiveTracks(trackIdsIn: IntArray?) {
        withBindingId { id, service ->
            this.trackIdsIn = trackIdsIn
            logger.info { "cv CvDetectionApiBinding.kt setting trackIdsIn=$trackIdsIn" }
            service.setPoseActiveTracks(id, trackIdsIn)
        }
    }

    override suspend fun setPoseLogsEnable(logsEnable: Boolean) {
        withBindingId { id, service ->
            this.logsEnable = logsEnable
            logger.info { "cv CvDetectionApiBinding.kt logsEnable=$logsEnable" }
            service.setPoseLogsEnable(id, logsEnable)
        }
    }

    override suspend fun getVersion(): String? {
        logger.info { "get version" }
        return binderHelper.execute { service -> service.version }
    }

    override suspend fun getServiceInfo(): ServiceInfo? {
        logger.info { "get service info" }
        return binderHelper.execute { service -> service.serviceInfo }
    }

    override fun observeIsMirrorDetected(): Flow<Boolean> {
        return merge(
            binderHelper.binderStateFlow
                .mapNotNull { if (it != BinderState.CONNECTED) false else null },
            isMirrorDetectedSharedFlow.onEach { logger.verbose { "mirror: $it" } }
        )
    }

    override fun observeHumans(aspects: Set<HumansDetectionAspect>): Flow<Humans> {
        return if (aspects.isEmpty()) {
            emptyFlow()
        } else {
            val subscriptionToken = getSubscriptionToken()
            val aspectsBeforeAdding = humanRecognitionsAspects
            humansSubscriptions[subscriptionToken] = aspects
            val aspectsAfterAdding = humanRecognitionsAspects

            merge(
                binderHelper.binderStateFlow
                    .mapNotNull { if (it != BinderState.CONNECTED) Humans.EMPTY else null },
                humansSharedFlow
                    .onSubscription {
                        onHumansSubscription(
                            subscriptionToken = subscriptionToken,
                            aspectsBeforeAdding = aspectsBeforeAdding,
                            aspectsAfterAdding = aspectsAfterAdding
                        )
                    }
                    .onCompletion { onHumansCompletion(subscriptionToken, aspects) }
                    .map { detectionEntity ->
                        detectionEntity.toHumans(humanRecognitionsAspects, lastHumans)
                            ?: Humans.EMPTY
                    }
            )
                .map {
                    lastHumans = it
                    it
                }
        }
    }

    private suspend fun onHumansSubscription(
        subscriptionToken: String,
        aspectsBeforeAdding: Set<HumansDetectionAspect>,
        aspectsAfterAdding: Set<HumansDetectionAspect>
    ) {
        humansMutex.withLock {
            if (aspectsBeforeAdding.isEmpty()) {
                logger.debug { "$subscriptionToken: subscribed due to creation of callback flow" }
            } else {
                if (aspectsBeforeAdding != aspectsAfterAdding) {
                    logger.debug { "$subscriptionToken: changed aspects $aspectsAfterAdding, resubscribe" }
                    withBindingId { id, service ->
                        service.subscribeForHumansDetection(
                            id,
                            humansListener,
                            aspectsAfterAdding.map { it.code }.toByteArray()
                        )
                    }
                } else {
                    logger.debug { "$subscriptionToken: unchanged aspects $aspectsAfterAdding, skip" }
                }
            }
        }
    }

    private suspend fun onHumansCompletion(
        subscriptionToken: String,
        aspects: Set<HumansDetectionAspect>
    ) {
        humansMutex.withLock {
            val aspectsBeforeRemoval = humanRecognitionsAspects
            humansSubscriptions.remove(subscriptionToken, aspects)
            val aspectsAfterRemoval = humanRecognitionsAspects
            if (aspectsAfterRemoval.isEmpty()) {
                logger.debug { "$subscriptionToken: unsubscribed due to creation of callback flow" }
            } else {
                if (aspectsBeforeRemoval != aspectsAfterRemoval) withBindingId { id, service ->
                    logger.debug {
                        "$subscriptionToken: still observing humans, do not stop callback yet," +
                                " but update observed aspects $aspectsAfterRemoval"
                    }
                    service.subscribeForHumansDetection(
                        id,
                        humansListener,
                        aspectsAfterRemoval.map { it.code }.toByteArray()
                    )
                }
            }
        }
    }

    override fun observeGestures(): Flow<Gesture> {
        return gestureSharedFlow
    }

    private fun withBindingId(action: (Int, ICvDetectionService) -> Unit) {
        coroutineScope.launch {
            val id = bindingId.filterNotNull().first()
            binderHelper.execute { service ->
                action.invoke(id, service)
            }
        }
    }
}

internal fun getBindingCvApiVersion(): String {
    return BuildConfig.CV_API_VERSION
}

internal fun getSubscriptionToken(): String {
    return UUID.randomUUID().toString()
}
