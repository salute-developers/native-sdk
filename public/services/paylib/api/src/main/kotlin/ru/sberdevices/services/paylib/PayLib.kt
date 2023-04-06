package ru.sberdevices.services.paylib

import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.services.paylib.entities.PayStatus

/**
 * Интерфейс взаимодействия со сценарием оплаты.
 */
interface PayLib {

    /**
     * Запуск оплаты. Подробнее об этапах оплаты см. по ссылке: https://developers.sber.ru/docs/ru/va/how-to/monetization/payments/smartpay/processing/payment-steps
     *
     * @param invoiceId Идентификатор созданного счета.
     * @return Результат оплаты.
     */
    @SinceVersion(1)
    suspend fun launchPayDialog(invoiceId: String): Result<PayStatus>

    /**
     * Выдает версию сервиса, установленного на устройстве.
     * @return Выдаст [Int.MAX_VALUE], если сервис на устройстве найден,
     * но у него нет версии - в таком случае совместимость вызываемых методов
     * не гарантируется. Если сервис не установлен на устройстве, то выдаст null.
     * Во всех остальных случаях выдает [Int] - значение версии сервиса.
     */
    fun getVersion(): Int?
}
