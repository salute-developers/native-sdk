package ru.sberdevices.pub.demoapp.ui.main

import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import org.koin.android.ext.android.inject
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.pub.demoapp.repository.Gesture
import ru.sberdevices.pub.demoapp.repository.GesturesRepository
import ru.sberdevices.services.pub.demoapp.R
import ru.sberdevices.utils.OnSwipeListener
import ru.sberdevices.utils.SWIPE_GESTURE_DEVICE_ID

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val logger = Logger.get("MainActivity")

    private val gesturesRepository: GesturesRepository by inject()

    private val gestureDetector by lazy {
        GestureDetectorCompat(
            this,
            OnSwipeListener { direction ->
                logger.debug { "onSwipe, direction: $direction" }

                Toast.makeText(this, "$direction", Toast.LENGTH_SHORT).show()
                gesturesRepository.onGestureDetected(
                    when (direction) {
                        OnSwipeListener.SwipeDirection.UP -> Gesture.SWIPE_UP
                        OnSwipeListener.SwipeDirection.DOWN -> Gesture.SWIPE_DOWN
                        OnSwipeListener.SwipeDirection.LEFT -> Gesture.SWIPE_LEFT
                        OnSwipeListener.SwipeDirection.RIGHT -> Gesture.SWIPE_RIGHT
                    }
                )
                true
            }
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.deviceId == SWIPE_GESTURE_DEVICE_ID) {
            return if (!gesturesRepository.gestureNavigationEnabledFlow.value) {
                true
            } else {
                gestureDetector.onTouchEvent(ev)
            }
        }

        return super.dispatchTouchEvent(ev)
    }
}
