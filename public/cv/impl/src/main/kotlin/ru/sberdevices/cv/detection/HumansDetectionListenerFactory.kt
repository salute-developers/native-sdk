package ru.sberdevices.cv.detection

import ru.sberdevices.common.logger.Logger
import ru.sberdevices.cv.IDeathListener
import ru.sberdevices.cv.detection.entity.IHumansDetectionListener

/**
 * Фабрика, создающая обертку над [IHumansDetectionListener]
 * Нужна для удобства тестирования
 */
interface HumansDetectionListenerFactory {
    fun getListener(onUpdate: (detectionEntity: ByteArray) -> Unit): IHumansDetectionListener
}

class HumansDetectionListenerFactoryImpl : HumansDetectionListenerFactory {
    private val logger = Logger.get("HumansDetectionListenerFactoryImpl")

    override fun getListener(onUpdate: (detectionEntity: ByteArray) -> Unit): IHumansDetectionListener {
        return object : IHumansDetectionListener.Stub() {
            init {
                logger.verbose { "create humans listener stub $this" }
            }

            override fun onUpdate(detectionEntity: ByteArray) {
                onUpdate.invoke(detectionEntity)
            }
        }
    }
}