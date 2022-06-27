package ru.sberdevices.cv.detection

import ru.sberdevices.cv.IDeathListener

/**
 * Фабрика, создающая обертку над [IDeathListener]
 * Нужна для удобства тестирования
 */
interface DeathListenerFactory {
    fun getListener(): IDeathListener
}

class DeathListenerFactoryImpl : DeathListenerFactory {

    override fun getListener(): IDeathListener {
        return object : IDeathListener.Stub() {
            override fun onDeath(bindingId: Int) {
                /** Do nothing */
            }
        }
    }
}