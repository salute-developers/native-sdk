package ru.sberdevices.cv.detection

import ru.sberdevices.common.logger.Logger
import ru.sberdevices.cv.IDeathListener
import ru.sberdevices.cv.detection.entity.IGestureDetectionListener

/**
 * Фабрика, создающая обертку над [IGestureDetectionListener]
 * Нужна для удобства тестирования
 */
interface GestureDetectionListenerFactory {
    fun getListener(onUpdate: (detectionEntity: ByteArray) -> Unit): IGestureDetectionListener
}

class GestureDetectionListenerFactoryImpl : GestureDetectionListenerFactory {
    private val logger = Logger.get("GestureDetectionListenerFactoryImpl")

    override fun getListener(onUpdate: (detectionEntity: ByteArray) -> Unit): IGestureDetectionListener {
        return object : IGestureDetectionListener.Stub() {
            init {
                logger.verbose { "create gestures listener stub $this" }
            }

            override fun onUpdate(detectionEntity: ByteArray) {
                onUpdate.invoke(detectionEntity)
            }
        }
    }
}