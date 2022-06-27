package ru.sberdevices.cv.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.sberdevices.common.logger.Logger

interface BindingIdStorage {
    val bindingId: StateFlow<Int?>
    fun set(bindingId: Int)
    fun get(): Int?
}

class BindingIdStorageImpl : BindingIdStorage {

    private val logger = Logger.get("BindingIdStorageImpl")

    private val _bindingId = MutableStateFlow<Int?>(null)

    override val bindingId = _bindingId.asStateFlow()

    override fun set(bindingId: Int) {
        val updated = _bindingId.compareAndSet(expect = null, update = bindingId)
        logger.debug { "Binding id ${if (updated) "" else "not "}updated to $bindingId" }
    }

    override fun get(): Int? {
        return _bindingId.value
    }
}
