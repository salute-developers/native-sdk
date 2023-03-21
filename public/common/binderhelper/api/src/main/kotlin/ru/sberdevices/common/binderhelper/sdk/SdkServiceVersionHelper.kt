package ru.sberdevices.common.binderhelper.sdk

import android.os.IInterface
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.logger.Logger

/**
 * Выдает версию сервиса, установленного на устройстве.
 * @param logger Логгер, используемый SDK, запрашивающего версию.
 * @return Выдаст [Int.MAX_VALUE], если сервис на устройстве найден,
 * но у него нет версии - в таком случае совместимость вызываемых методов
 * не гарантируется. Если сервис не установлен на устройстве, то выдаст null.
 * Во всех остальных случаях выдает [Int] - значение версии сервиса.
 */
fun <T : IInterface> BinderHelper<T>.getVersionForSdk(logger: Logger? = null): Int? {
    if (!hasService()) {
        logger?.error { "Service not exists!" }
        return null
    }

    return getVersion().fold(onFailure = {
        logger?.error(it) { "Version not found!" }
        Int.MAX_VALUE
    }, onSuccess = {
        logger?.debug { "Service version: $it" }
        it
    })
}
