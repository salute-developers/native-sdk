package ru.sberdevices.cv.detection

import ru.sberdevices.common.logger.Logger
import ru.sberdevices.cv.IDeathListener
import ru.sberdevices.cv.detection.entity.IMirrorDetectedListener

/**
 * Фабрика, создающая обертку над [IMirrorDetectedListener]
 * Нужна для удобства тестирования
 */
interface MirrorDetectedListenerFactory {
    fun getListener(onUpdate: (Boolean) -> Unit): IMirrorDetectedListener
}

class MirrorDetectedListenerFactoryImpl : MirrorDetectedListenerFactory {
    private val logger = Logger.get("MirrorDetectedListenerFactoryImpl")

    override fun getListener(onUpdate: (detected: Boolean) -> Unit): IMirrorDetectedListener {
        return object : IMirrorDetectedListener.Stub() {
            init {
                logger.verbose { "create is mirror detected listener stub $this" }
            }

            override fun onUpdate(detected: Boolean) {
                onUpdate.invoke(detected)
            }
        }
    }
}