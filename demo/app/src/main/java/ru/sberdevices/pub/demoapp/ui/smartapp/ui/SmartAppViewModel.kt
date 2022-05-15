package ru.sberdevices.pub.demoapp.ui.smartapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID.randomUUID
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.messaging.MessageId
import ru.sberdevices.messaging.Messaging
import ru.sberdevices.messaging.Payload
import ru.sberdevices.pub.demoapp.ui.smartapp.model.BaseCommand
import ru.sberdevices.pub.demoapp.ui.smartapp.model.BuyItems
import ru.sberdevices.pub.demoapp.ui.smartapp.model.ClearClothesCommand
import ru.sberdevices.pub.demoapp.ui.smartapp.model.Clothes
import ru.sberdevices.pub.demoapp.ui.smartapp.model.Invoice
import ru.sberdevices.pub.demoapp.ui.smartapp.model.MyAppState
import ru.sberdevices.pub.demoapp.ui.smartapp.model.Order
import ru.sberdevices.pub.demoapp.ui.smartapp.model.OrderInfo
import ru.sberdevices.pub.demoapp.ui.smartapp.model.Quantity
import ru.sberdevices.pub.demoapp.ui.smartapp.model.ShoppingCartItem
import ru.sberdevices.pub.demoapp.ui.smartapp.model.WearThisCommand
import ru.sberdevices.services.appstate.AppStateHolder
import ru.sberdevices.services.paylib.PayLib
import ru.sberdevices.services.paylib.entities.PayResultCode
import ru.sberdevices.pub.demoapp.ui.smartapp.network.NetworkClient

/**
 * In this example view model gets messages from smartapp backend by [Messaging].
 * Also it shares its state with smartapp backend via [AppStateHolder].
 */
class SmartAppViewModel(
    messaging: Messaging,
    private val paylib: PayLib,
    private val appStateHolder: AppStateHolder,
    private val ioCoroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val commandParser = Json {
        classDiscriminator = "command"
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val networkClient: NetworkClient = NetworkClient()
    private val logger by Logger.lazy("SmartAppViewModel")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val currentClothes: MutableSet<Clothes> = HashSet()

    private val _clothes = MutableSharedFlow<Clothes?>(
        replay = Clothes.values().size
    )
    private val _buyItems = MutableSharedFlow<BuyItems>(
        replay = 1
    )

    val buyItems: SharedFlow<BuyItems> = _buyItems.asSharedFlow()
    val clothes = _clothes.asSharedFlow()

    private val listener = object : Messaging.Listener {
        override fun onMessage(messageId: MessageId, payload: Payload) {
            logger.debug { "Message ${messageId.value} received: ${payload.data}" }

            val model = commandParser.decodeFromString<BaseCommand>(payload.data)

            when (model) {
                is WearThisCommand -> {
                    model.clothes?.let {
                        currentClothes.add(it)
                        _clothes.tryEmit(model.clothes)
                    }
                }
                is ClearClothesCommand -> {
                    currentClothes.clear()
                    _clothes.resetReplayCache()
                    _clothes.tryEmit(null)
                }
            }

            // send current state to smartapp backend
            appStateHolder.setState(
                Json.encodeToString(
                    MyAppState("На андроиде ${currentClothes.joinToString(transform = { it.clothes })}")
                )
            )
        }

        override fun onError(messageId: MessageId, throwable: Throwable) {
            logger.error { throwable.stackTraceToString() }
        }
    }

    init {
        messaging.addListener(listener)
    }

    fun addItemsToCartAndPay() {
        viewModelScope.launch(ioCoroutineDispatcher) {

            // ТОЛЬКО ДЛЯ ДЕМО
            // Рекомендуется регистрировать счет в SmartPay через собственный бэкэнд и возвращать invoice_id в клиент.
            val invoiceId = getInvoiceIdForTestOrder()
            if (invoiceId != null) {

                // Запускаем интерфейс оплаты на устройстве.
                paylib.launchPayDialog(invoiceId).onSuccess {
                    logger.info { "paydialog completed: $it" }

                    // Проверяем результат работы интерфейса платежа (успешно/ошибка или пользователь отменил платеж)
                    if (it.resultCode == PayResultCode.SUCCESS) {
                        _buyItems.tryEmit(BuyItems.ELEPHANT)
                    }

                }.onFailure {
                    logger.error { "paydialog failed: $it" }
                }
            }
        }
    }

    /**
     * Создание **тестового** счета на оплату
     *
     * (только для демонстрационных целей)
     * @return InvoiceId идентификатор нового счета
     */
    private fun getInvoiceIdForTestOrder(): String? {
        // Создаем тестовый счет на оплату
        val invoiceOrder = createInvoiceOrder()
        // Регистрируем счет в SmartPay API
        return networkClient.createInvoice(invoiceOrder)?.invoice_id
    }

    /**
     * Пример счета с корзиной и единственным товаром в ней
     */
    private fun createInvoiceOrder(): Invoice {
        val cartItem = ShoppingCartItem(
            1,
            name = "New Elephant",
            item_price = 100,
            item_amount = 100, // must be item_price*quantity.value,
            item_code = PAYLIB_ITEM_CODE,
            quantity = Quantity(
                1,
                "thing")
        )
        val orderInfo = OrderInfo(
            order_id = randomUUID().toString(),
            order_date =  ZonedDateTime.now().format(dateTimeFormatter),
            amount = cartItem.item_amount,
            purpose = PAYLIB_ORDER_PURPOSE,
            description = PAYLIB_ORDER_DESCRIPTION,
            service_id = PAYLIB_TEST_SERVICE_ID,
            order_bundle = listOf(cartItem)
        )
        return Invoice(
            invoice = Order(
                order = orderInfo
            )
        )
    }

    private companion object {
        /**
         * Уникальный Номер (идентификатор) товарной позиции в системе магазина.
         */
        const val PAYLIB_ITEM_CODE = "ru.some.elephant"

        /**
         * Краткое назначение платежа. Отображается при оплате/подтверждении безакцептного списания клиентом
         */
        const val PAYLIB_ORDER_PURPOSE = "Покупка слона"

        /**
         * Описание платежа для отображения клиенту.
         */
        const val PAYLIB_ORDER_DESCRIPTION = "Покупка слона, OOO Elephant Seller"

        /**
         * Идентификатор для категории оплаченных товаров и услуг
         * @see <a href="https://developers.sber.ru/docs/ru/va/how-to/monetization/payments/access">Тестовый Service_id</a>
         */
        const val PAYLIB_TEST_SERVICE_ID = "27"
    }
}


