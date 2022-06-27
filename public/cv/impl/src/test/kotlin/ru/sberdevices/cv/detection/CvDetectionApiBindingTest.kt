@file:Suppress("MaxLineLength", "ForbidDefaultCoroutineDispatchers")

package ru.sberdevices.cv.detection

import android.os.IBinder
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.entities.BinderState
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.cv.ICvDetectionService
import ru.sberdevices.cv.IDeathListener
import ru.sberdevices.cv.ServiceInfo
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
import kotlin.test.assertEquals

/**
 * @author Ирина Карпенко on 03.02.2022
 */
class CvDetectionApiBindingTest {

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.flow.FlowKt")
        mockkStatic("ru.sberdevices.cv.detection.CvDetectionApiBindingKt")
        mockkStatic("ru.sberdevices.cv.detection.util.BytesConvertersKt")
        mockkStatic(UUID::class)

        every { getBindingCvApiVersion() } answers { "0.0.0" }
        every { ByteArray(0).toGesture() } answers { Gesture(Gesture.Type.OK, null, 0) }
        every { ByteArray(0).toHumans(any(), any()) } answers { Humans.EMPTY }
    }

    @Test
    fun `when created, then subscribes to service connection lifecycle events`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            assertEquals(1, binderStateFlow.subscriptionCount.value)
        }
    }

    @Test
    fun `when created, then calls connect`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            verify { binderHelper.connect() }
        }
    }

    @Test
    fun `given no service on device, when created, then does not call connect`() = runBlockingTest {
        // Arrange
        with(TestObjects(hasService = false)) {

            // Act

            // Assert
            verify(exactly = 0) { binderHelper.connect() }
        }
    }

    @Test
    fun `given no detection setup, when connected, then does not restore detection`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
            }
        }
    }

    @Test
    fun `when created, then generated binding id`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            verify { binder.bindingId }
        }
    }

    @Test
    fun `when created, then sets generated binding id to storage`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            verify { bindingIdStorage.set(generatedBindingId) }
        }
    }

    @Test
    fun `when created, then sends death listener`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            bindingIds.tryEmit(generatedBindingId)

            // Act

            // Assert
            coVerify { binder.sendDeathListener(generatedBindingId, deathListener) }
        }
    }

    @Test
    fun `when created, then sends client cv api version`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act

            // Assert
            verify { binder.sendClientCvApiVersion(generatedBindingId, "0.0.0") }
        }
    }

    @Test
    fun `when cv api version requested, then sends service cv api version`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act
            val serviceVersion = binding.getVersion()

            // Assert
            assertEquals(version, serviceVersion)
        }
    }

    @Test
    fun `when service info requested, then sends service info`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {

            // Act
            val actualServiceInfo = binding.getServiceInfo()

            // Assert
            assertEquals(serviceInfo, actualServiceInfo)
        }
    }

    @Test
    fun `when some humans aspects are observed, then calls subscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob = launch { binding.observeHumans(aspects).collect() }

            // Assert
            coVerify { binder.subscribeForHumansDetection(1, humansDetectionListener, aspects.map { it.code }.toByteArray()) }
            humansJob.cancel()
        }
    }

    @Test
    fun `when gestures are observed, then calls subscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val gesturesJob = launch { binding.observeGestures().collect() }

            // Assert
            coVerify { binder.subscribeForGestureDetection(1, gestureDetectionListener) }
            gesturesJob.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to gestures, then calls subscribe only once`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val gesturesJob1 = launch { binding.observeGestures().collect() }

            every { getSubscriptionToken() } returns "token2"
            val gesturesJob2 = launch { binding.observeGestures().collect() }

            // Assert
            coVerify(exactly = 1) {
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
            }
            gesturesJob1.cancel()
            gesturesJob2.cancel()
        }
    }

    @Test
    fun `when all subscriptions to gestures cancelled, then calls unsubscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val gesturesJob1 = launch { binding.observeGestures().collect() }

            every { getSubscriptionToken() } returns "token2"
            val gesturesJob2 = launch { binding.observeGestures().collect() }

            // Act
            gesturesJob1.cancel()
            gesturesJob2.cancel()

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
                binder.unsubscribeFromGestureDetection(1, gestureDetectionListener)
            }
        }
    }

    @Test
    fun `when subscription to gestures cancelled and renewed, then correct binder calls`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val gesturesJob1 = launch { binding.observeGestures().collect() }
            val gestureDetectionListener2: IGestureDetectionListener = mockk {
                every { asBinder() } returns gestureDetectionIBinder
            }
            var gesturesJob2: Job? = null
            gesturesJob1.invokeOnCompletion {
                every { gestureDetectionListenerFactory.getListener(capture(bytesListenerSlot)) } returns gestureDetectionListener2
                every { getSubscriptionToken() } returns "token2"
                gesturesJob2 = launch { binding.observeGestures().collect() }
            }

            // Act
            gesturesJob1.cancel()

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
                binder.unsubscribeFromGestureDetection(1, gestureDetectionListener)
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener2)
            }
            gesturesJob2?.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to gestures, then emits only one gesture`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val collectedGestures = mutableListOf<Gesture>()
            val gesturesJob1 = launch { binding.observeGestures().collect { collectedGestures.add(it) } }

            every { getSubscriptionToken() } returns "token2"
            val gesturesJob2 = launch { binding.observeGestures().collect() }
            gestureDetectionListener.onUpdate(ByteArray(0))

            // Assert
            assertEquals(1, collectedGestures.count())
            gesturesJob1.cancel()
            gesturesJob2.cancel()
        }
    }

    @Test
    fun `when mirror is observed, then calls subscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val mirrorStatusesJob = launch { binding.observeIsMirrorDetected().collect() }

            // Assert
            coVerify { binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener) }
            mirrorStatusesJob.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to mirror, then calls subscribe only once`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val mirrorStatusesJob1 = launch { binding.observeIsMirrorDetected().collect() }

            every { getSubscriptionToken() } returns "token2"
            val mirrorStatusesJob2 = launch { binding.observeIsMirrorDetected().collect() }

            // Assert
            coVerify(exactly = 1) {
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
            }
            mirrorStatusesJob1.cancel()
            mirrorStatusesJob2.cancel()
        }
    }

    @Test
    fun `when all subscriptions to mirror cancelled, then calls unsubscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val mirrorJob1 = launch { binding.observeIsMirrorDetected().collect() }

            every { getSubscriptionToken() } returns "token2"
            val mirrorJob2 = launch { binding.observeIsMirrorDetected().collect() }

            // Act
            mirrorJob1.cancel()
            mirrorJob2.cancel()

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
                binder.unsubscribeFromIsMirrorDetected(1, mirrorDetectionListener)
            }
        }
    }

    @Test
    fun `when subscription to mirror cancelled and renewed, then correct binder calls`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val mirrorJob1 = launch { binding.observeIsMirrorDetected().collect() }
            val mirrorDetectionListener2: IMirrorDetectedListener = mockk {
                every { asBinder() } returns mirrorDetectionIBinder
            }
            var mirrorJob2: Job? = null
            mirrorJob1.invokeOnCompletion {
                every { mirrorDetectedListenerFactory.getListener(capture(boolListenerSlot)) } returns mirrorDetectionListener2
                every { getSubscriptionToken() } returns "token2"
                mirrorJob2 = launch { binding.observeIsMirrorDetected().collect() }
            }

            // Act
            mirrorJob1.cancel()

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
                binder.unsubscribeFromIsMirrorDetected(1, mirrorDetectionListener)
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener2)
            }
            mirrorJob2?.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to mirror, then emits only one status update`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val collectedMirrorStatuses = mutableListOf<Boolean>()
            val mirrorStatusesJob1 = launch { binding.observeIsMirrorDetected().collect { collectedMirrorStatuses.add(it) } }

            every { getSubscriptionToken() } returns "token2"
            val mirrorStatusesJob2 = launch { binding.observeIsMirrorDetected().collect() }
            mirrorDetectionListener.onUpdate(true)

            // Assert
            assertEquals(1, collectedMirrorStatuses.count())
            mirrorStatusesJob1.cancel()
            mirrorStatusesJob2.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to humans with different aspects, then calls subscribe 2 times`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }

            every { getSubscriptionToken() } returns "token2"

            val aspects2 = setOf(HumansDetectionAspect.Face.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }

            // Assert
            coVerify(exactly = 1) { binder.subscribeForHumansDetection(1, humansDetectionListener, aspects1.map { it.code }.toByteArray()) }
            coVerify(exactly = 1) { binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox, HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray()) }
            coVerify(exactly = 1) { humansDetectionListenerFactory.getListener(any()) }
            coVerify(exactly = 0) { binder.unsubscribeFromHumansDetection(1, humansDetectionListener) }
            humansJob1.cancel()
            humansJob2.cancel()
        }
    }

    @Test
    fun `when there are two subscriptions to humans with same aspects, then calls subscribe 1 time`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }

            every { getSubscriptionToken() } returns "token2"

            val aspects2 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }

            // Assert
            coVerify(exactly = 1) { binder.subscribeForHumansDetection(1, humansDetectionListener, aspects1.map { it.code }.toByteArray()) }
            coVerify(exactly = 1) { humansDetectionListenerFactory.getListener(any()) }
            coVerify(exactly = 0) { binder.unsubscribeFromHumansDetection(1, humansDetectionListener) }
            humansJob1.cancel()
            humansJob2.cancel()
        }
    }

    @Test
    fun `when empty humans aspects are observed, then does not call subscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects = emptySet<HumansDetectionAspect>()
            val humansJob = launch { binding.observeHumans(aspects).collect() }

            // Assert
            coVerify(exactly = 0) { binder.subscribeForHumansDetection(1, humansDetectionListener, any()) }
            humansJob.cancel()
        }
    }

    @Test
    fun `when empty humans aspects subscription cancelled, then does not call unsubscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects = emptySet<HumansDetectionAspect>()
            val humansJob = launch { binding.observeHumans(aspects).collect() }
            humansJob.cancel()

            // Assert
            coVerify(exactly = 0) { binder.unsubscribeFromHumansDetection(1, humansDetectionListener) }
        }
    }

    @Test
    fun `when subscription to humans completed and 0 subscribers left, then calls unsubscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob = launch { binding.observeHumans(aspects1).collect() }
            humansJob.cancel()

            // Assert
            coVerify(exactly = 1) { binder.unsubscribeFromHumansDetection(1, humansDetectionListener) }
        }
    }

    @Test
    fun `when subscription to humans completed and 1 subscribers left, then does not call unsubscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }
            every { getSubscriptionToken() } returns "token2"
            val aspects2 = setOf(HumansDetectionAspect.Face.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }

            humansJob1.cancel()

            // Assert
            coVerify(exactly = 0) { binder.unsubscribeFromHumansDetection(1, humansDetectionListener) }
            humansJob2.cancel()
        }
    }

    @Test
    fun `when subscription to humans completed and 1 subscribers left, then calls subscribe`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }
            every { getSubscriptionToken() } returns "token2"
            val aspects2 = setOf(HumansDetectionAspect.Face.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }
            humansJob1.cancel()

            // Assert
            coVerifyOrder {
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox).map { it.code }.toByteArray())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox, HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
            }
            humansJob2.cancel()
        }
    }

    @Test
    fun `when subscription to humans completed and 1 subscribers left, then does not call subscribe if aspects are same`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"

            // Act
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }
            every { getSubscriptionToken() } returns "token2"
            val aspects2 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }
            humansJob1.cancel()

            // Assert
            coVerify(exactly = 1) {
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox).map { it.code }.toByteArray())
            }
            humansJob2.cancel()
        }
    }

    @Test
    fun `when humans job cancelled and then launched again, then correct binder calls`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }
            var humansJob2: Job? = null
            humansJob1.invokeOnCompletion {
                println("humans job 1 completed")
                val aspects2 = setOf(HumansDetectionAspect.Face.BoundingBox)
                humansJob2 = launch { binding.observeHumans(aspects2).collect() }
            }

            every { getSubscriptionToken() } returns "token2"
            val humansDetectionListener2: IHumansDetectionListener = mockk {
                every { asBinder() } returns humansDetectionIBinder
            }
            every { humansDetectionListenerFactory.getListener(any()) } returns humansDetectionListener2

            // Act
            humansJob1.cancel()

            // Assert
            coVerifyOrder {
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox).map { it.code }.toByteArray())
                binder.unsubscribeFromHumansDetection(1, humansDetectionListener)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener2, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
            }
            humansJob2?.cancel()
        }
    }

    @Test
    fun `when humans job cancelled and immediately launched again, then correct binder calls`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects1 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob1 = launch { binding.observeHumans(aspects1).collect() }
            humansJob1.invokeOnCompletion {
                println("humans job 1 completed")
            }

            every { getSubscriptionToken() } returns "token2"
            val humansDetectionListener2: IHumansDetectionListener = mockk {
                every { asBinder() } returns humansDetectionIBinder
            }
            every { humansDetectionListenerFactory.getListener(any()) } returns humansDetectionListener2

            // Act
            humansJob1.cancel()
            val aspects2 = setOf(HumansDetectionAspect.Body.BoundingBox)
            val humansJob2 = launch { binding.observeHumans(aspects2).collect() }

            // Assert
            coVerifyOrder {
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Body.BoundingBox).map { it.code }.toByteArray())
                binder.unsubscribeFromHumansDetection(1, humansDetectionListener)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener2, setOf(HumansDetectionAspect.Body.BoundingBox).map { it.code }.toByteArray())
            }
            humansJob2.cancel()
        }
    }

    @Test
    fun `when closed, then correct closing procedure`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects = setOf(HumansDetectionAspect.Face.BoundingBox)
            val humansJob = launch { binding.observeHumans(aspects).collect() }
            val gesturesJob = launch { binding.observeGestures().collect() }
            val mirrorJob = launch { binding.observeIsMirrorDetected().collect() }

            // Act
            binding.close()

            // Assert
            coVerifyOrder {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
                binder.unsubscribeFromHumansDetection(1, humansDetectionListener)
                binder.unsubscribeFromGestureDetection(1, gestureDetectionListener)
                binder.unsubscribeFromIsMirrorDetected(1, mirrorDetectionListener)
                binderHelper.disconnect()
            }
            humansJob.cancel()
            gesturesJob.cancel()
            mirrorJob.cancel()
        }
    }

    @Test
    fun `when disconnected, then sends default detections`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects = setOf(HumansDetectionAspect.Face.BoundingBox)
            val collectedHumans = mutableListOf<Humans>()
            val humansJob = launch { binding.observeHumans(aspects).collect { collectedHumans.add(it) } }
            val collectedMirrorStatuses = mutableListOf<Boolean>()
            val mirrorStatusesJob = launch { binding.observeIsMirrorDetected().collect { collectedMirrorStatuses.add(it) } }

            // Act
            binderStateFlow.emit(BinderState.DISCONNECTED)

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
            }
            assertEquals(listOf(Humans.EMPTY), collectedHumans)
            assertEquals(listOf(false), collectedMirrorStatuses)
            humansJob.cancel()
            mirrorStatusesJob.cancel()
        }
    }

    @Test
    fun `when null binding, then sends default detections`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects = setOf(HumansDetectionAspect.Face.BoundingBox)
            val collectedHumans = mutableListOf<Humans>()
            val humansJob = launch { binding.observeHumans(aspects).collect { collectedHumans.add(it) } }
            val collectedMirrorStatuses = mutableListOf<Boolean>()
            val mirrorStatusesJob = launch { binding.observeIsMirrorDetected().collect { collectedMirrorStatuses.add(it) } }

            // Act
            binderStateFlow.emit(BinderState.NULL_BINDING)

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
            }
            assertEquals(listOf(Humans.EMPTY), collectedHumans)
            assertEquals(listOf(false), collectedMirrorStatuses)
            humansJob.cancel()
            mirrorStatusesJob.cancel()
        }
    }

    @Test
    fun `when binding died, then sends default detections`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects = setOf(HumansDetectionAspect.Face.BoundingBox)
            val collectedHumans = mutableListOf<Humans>()
            val humansJob = launch { binding.observeHumans(aspects).collect { collectedHumans.add(it) } }
            val collectedMirrorStatuses = mutableListOf<Boolean>()
            val mirrorStatusesJob = launch { binding.observeIsMirrorDetected().collect { collectedMirrorStatuses.add(it) } }

            // Act
            binderStateFlow.emit(BinderState.BINDING_DIED)

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
            }
            assertEquals(listOf(Humans.EMPTY), collectedHumans)
            assertEquals(listOf(false), collectedMirrorStatuses)
            humansJob.cancel()
            mirrorStatusesJob.cancel()
        }
    }

    @Test
    fun `when reconnected, then restores detection`() = runBlockingTest {
        // Arrange
        with(TestObjects()) {
            every { getSubscriptionToken() } returns "token1"
            val aspects = setOf(HumansDetectionAspect.Face.BoundingBox)
            val humansJob = launch { binding.observeHumans(aspects).collect() }
            val gesturesJob = launch { binding.observeGestures().collect() }
            val mirrorStatusesJob = launch { binding.observeIsMirrorDetected().collect() }

            // Act
            binderStateFlow.emit(BinderState.DISCONNECTED)
            binderStateFlow.emit(BinderState.CONNECTED)

            // Assert
            coVerifySequence {
                binder.bindingId
                deathListenerFactory.getListener()
                binder.sendDeathListener(1, deathListener)
                binder.sendClientCvApiVersion(1, version)
                humansDetectionListenerFactory.getListener(any())
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                gestureDetectionListenerFactory.getListener(any())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
                mirrorDetectedListenerFactory.getListener(any())
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
                binder.subscribeForHumansDetection(1, humansDetectionListener, setOf(HumansDetectionAspect.Face.BoundingBox).map { it.code }.toByteArray())
                binder.subscribeForGestureDetection(1, gestureDetectionListener)
                binder.subscribeForIsMirrorDetected(1, mirrorDetectionListener)
            }
            humansJob.cancel()
            gesturesJob.cancel()
            mirrorStatusesJob.cancel()
        }
    }


    @After
    fun tearDown() {
        unmockkAll()
    }

    @Suppress("LongParameterList")
    private inner class TestObjects(
        val serviceInfo: ServiceInfo = mockk {},
        val initialBindingId: Int? = null,
        val generatedBindingId: Int = 1,
        val version: String = "0.0.0",
        val binder: ICvDetectionService = mockk {
            every { this@mockk.bindingId } answers { generatedBindingId }
            every { this@mockk.version } answers { version }
            every { this@mockk.serviceInfo } answers { serviceInfo }
            every { this@mockk.sendDeathListener(any(), any()) } just Runs
            every { this@mockk.sendClientCvApiVersion(any(), any()) } just Runs
            every { this@mockk.subscribeForHumansDetection(any(), any(), any()) } just Runs
            every { this@mockk.subscribeForGestureDetection(any(), any()) } just Runs
            every { this@mockk.unsubscribeFromHumansDetection(any(), any()) } just Runs
            every { this@mockk.unsubscribeFromGestureDetection(any(), any()) } just Runs
            every { this@mockk.subscribeForIsMirrorDetected(any(), any()) } just Runs
            every { this@mockk.unsubscribeFromIsMirrorDetected(any(), any()) } just Runs
        },
        val binderStateFlow: MutableStateFlow<BinderState> = MutableStateFlow(BinderState.DISCONNECTED),
        val hasService: Boolean = true,
        val successfullyConnected: Boolean = true,
        val lambdaSlot: CapturingSlot<(ICvDetectionService) -> Any?> = slot(),
        val binderHelper: BinderHelper<ICvDetectionService> = mockk {
            every { this@mockk.binderStateFlow } answers { binderStateFlow }
            every { this@mockk.hasService() } answers { hasService }
            every { this@mockk.connect() } answers {
                binderStateFlow.tryEmit(if (successfullyConnected) BinderState.CONNECTED else BinderState.DISCONNECTED)
                successfullyConnected
            }
            every { this@mockk.disconnect() } answers { binderStateFlow.tryEmit(BinderState.DISCONNECTED) }
            coEvery { execute(capture(lambdaSlot)) } coAnswers { lambdaSlot.captured.invoke(binder) }
            every { tryExecute(capture(lambdaSlot)) } answers { lambdaSlot.captured.invoke(binder) }
        },
        val bindingIds: MutableStateFlow<Int?> = MutableStateFlow(initialBindingId),
        val bindingIdStorage: BindingIdStorage = mockk {
            every { this@mockk.get() } answers { initialBindingId }
            every { this@mockk.bindingId } answers { bindingIds }
            every { this@mockk.set(any()) } answers { bindingIds.tryEmit(generatedBindingId) }
        },
        val coroutineDispatchers: CoroutineDispatchers = object : CoroutineDispatchers {
            override val ui: CoroutineDispatcher = Dispatchers.Unconfined
            override val uiImmediate: CoroutineDispatcher = Dispatchers.Unconfined
            override val io: CoroutineDispatcher = Dispatchers.Unconfined
            override val default: CoroutineDispatcher = Dispatchers.Unconfined
            override val sequentialWork: CoroutineDispatcher = Dispatchers.Unconfined
        },
        val deathIBinder: IBinder = mockk(),
        val deathListener: IDeathListener = mockk {
            every { asBinder() } returns deathIBinder
        },
        val bytesListenerSlot: CapturingSlot<(ByteArray) -> Unit> = slot(),
        val gestureDetectionIBinder: IBinder = mockk(),
        val gestureDetectionListener: IGestureDetectionListener = mockk {
            every { asBinder() } returns gestureDetectionIBinder
            every { this@mockk.onUpdate(any()) } answers { bytesListenerSlot.captured.invoke(ByteArray(0)) }
        },
        val humansDetectionIBinder: IBinder = mockk(),
        val humansDetectionListener: IHumansDetectionListener = mockk {
            every { asBinder() } returns humansDetectionIBinder
            every { this@mockk.onUpdate(any()) } answers { bytesListenerSlot.captured.invoke(ByteArray(0)) }
        },
        val boolListenerSlot: CapturingSlot<(Boolean) -> Unit> = slot(),
        val mirrorDetectionIBinder: IBinder = mockk(),
        val isMirrorDetected: Boolean = false,
        val mirrorDetectionListener: IMirrorDetectedListener = mockk {
            every { asBinder() } returns mirrorDetectionIBinder
            every { this@mockk.onUpdate(any()) } answers { boolListenerSlot.captured.invoke(isMirrorDetected) }
        },
        val deathListenerFactory: DeathListenerFactory = mockk {
            every { this@mockk.getListener() } returns deathListener
        },
        val humansDetectionListenerFactory: HumansDetectionListenerFactory = mockk {
            every { this@mockk.getListener(capture(bytesListenerSlot)) } returns humansDetectionListener
        },
        val gestureDetectionListenerFactory: GestureDetectionListenerFactory = mockk {
            every { this@mockk.getListener(capture(bytesListenerSlot)) } returns gestureDetectionListener
        },
        val mirrorDetectedListenerFactory: MirrorDetectedListenerFactory = mockk {
            every { this@mockk.getListener(capture(boolListenerSlot)) } returns mirrorDetectionListener
        },
        val binding: CvDetectionApiBinding = CvDetectionApiBinding(
            bindingIdStorage = bindingIdStorage,
            binderHelper = binderHelper,
            coroutineDispatchers = coroutineDispatchers,
            deathListenerFactory = deathListenerFactory,
            humansDetectionListenerFactory = humansDetectionListenerFactory,
            gestureDetectionListenerFactory = gestureDetectionListenerFactory,
            mirrorDetectedListenerFactory = mirrorDetectedListenerFactory
        )
    )
}