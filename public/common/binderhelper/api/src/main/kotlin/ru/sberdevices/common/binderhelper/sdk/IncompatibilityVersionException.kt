package ru.sberdevices.common.binderhelper.sdk

/**
 * Исключение, выкидываемое при несовместимости версии сервиса в системе с вызываемым методом.
 * Может выбрасываться как при отсутствии сервиса в системе, так и в случаях, когда версия сервиса ниже требуемой.
 * @param message Подробное описание причины несовместимости.
 * @author Максим Митюшкин on 13.03.2023.
 */
class IncompatibilityVersionException(override val message: String) : RuntimeException(message)
