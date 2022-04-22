package ru.sberdevices.services.paylib

import ru.sberdevices.services.paylib.entities.PayStatus

/**
 * Интерфейс взаимодействия со сценарием оплаты.
 */
interface PayLib {

    /**
     * Запуск оплаты.
     * Подробнее об этапах оплаты см. по ссылке:
     * https://developers.sber.ru/docs/ru/smartservices/smartpay/processing/payment-steps
     *
     * @param invoiceId Идентификатор созданного счета.
     * @return Результат оплаты.
     */
    suspend fun launchPayDialog(invoiceId: String): Result<PayStatus>
}
