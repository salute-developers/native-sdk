package ru.sberdevices.pub.demoapp.ui.smartapp.model

import kotlinx.serialization.Serializable

/**
 * Базовая структура счета для создания заказа.
 * @see <a href="https://developers.sber.ru/docs/ru/va/reference/smartservices/smartpay/processing/smartpay-api">API SmartPay</a>
 */
@Serializable
data class Invoice(
    val invoice: Order
)

/**
 * Базовая структура для создания заказа
 */
@Serializable
data class Order(
    val order: OrderInfo
)

/**
 * Описание заказа
 */
@Serializable
data class OrderInfo(
    /**
     * Идентификатор заказа.
     * Должен быть уникален в рамках выделенного для приложения service_id, иначе не будет создан новый invoice_id
     */
    val order_id: String,

    /**
     * Дата и время заказа в формате RFC 3339
     */
    val order_date: String,

    /**
     * Сумма счета без разделителя, в копейках. Например, 1 рубль передается в этом поле как 100.
     * Если в запросе указывается корзина товаров, то это поле должно быть равно сумме стоимости всех товаров в корзине sum(order_bundle.item_amount)
     */
    val amount: Int,

    /**
     * Краткое назначение платежа
     */
    val purpose: String,

    /**
     * Описание платежа для отображения клиенту
     */
    val description: String,

    /**
     * Идентификатор сервиса, полученный при выдаче токена для авторизации запроса
     */
    val service_id: String,

    /**
     * Код валюты в формате ISO 4217.
     * Пока поддерживается только значение RUB
     */
    val currency: String = "RUB",

    /**
     * Язык текстовых полей в формате BCP 47.
     * Пока поддерживается только значение ru-RU
     */
    val language: String = "ru-RU",

    /**
     * Корзина покупок
     */
    val order_bundle: List<ShoppingCartItem>
)

/**
 * Товарная позиция из корзины покупок
 */
@Serializable
data class ShoppingCartItem(
    /**
     * Номер (идентификатор) товарной позиции в системе магазина. Параметр должен быть уникальным в рамках запроса
     */
    val position_id: Int,

    /**
     * Наименование или описание товарной позиции
     */
    val name: String,

    /**
     * Цена единицы товарной позиции. Указывается без разделителя, в копейках
     */
    val item_price: Int,

    /**
     * Количество и название сущности в которой считаются товары
     */
    val quantity: Quantity,

    /**
     * Общая цена всех единиц товарной позиции. Указывается без разделителя, в копейках
     */
    val item_amount: Int,

    /**
     * Код валюты в формате ISO 4217.
     */
    val currency: String = "RUB",

    /**
     * Номер (идентификатор) товарной позиции в системе магазина. Параметр должен быть уникальным в рамках запроса
     */
    val item_code: String
)

/**
 * Описание количественных характеристик определенной позиции корзины
 */
@Serializable
data class Quantity(
    /**
     * Количество товара в позиции
     */
    val value: Int,

    /**
     * Единица измерения товара в позиции
     */
    val measure: String
)

/**
 * Структура ответа SmartAPI на создание счета
 */
@Serializable
data class InvoiceResponse(
    /**
     * Результат работы API
     */
    val error: ResponseError,
    /**
     * Идентификатор нового счета
     */
    val invoice_id: String
)

/**
 * Базовая структура ответа SmartPay API
 */
@Serializable
data class ResponseError(
    /**
     * Код ответа
     * Если запрос выполнен усрешно, то всегда = '0'
     */
    val error_code: String,
    /**
     * Техническое описание ошибки
     */
    val error_description: String,
    /**
     * Сообщение для пользователя
     */
    val user_message: String
)