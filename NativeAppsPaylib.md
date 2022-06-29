# Прием платежей с библиотекой messaging

Если вы подключили прием платежей в Native App c помощью библиотеки **messaging**, используйте пример ниже — настройка одностадийного платежа в смартапе, созданном в [SmartApp Code](https://developers.sber.ru/docs/ru/va/reference/code/overview) с шаблоном [Монетизация для Native App](https://developers.sber.ru/docs/ru/va/reference/code/templates/smartapp-templates).

## Формирование заказа

Cформируйте корзину в методе `addItemsToCartAndPay` в соответствии с примером из [демо-приложения](https://github.com/salute-developers/native-sdk/tree/main/demo):

```
fun addItemsToCartAndPay() {
    val cardInfo = CardInfo(
        1,
        name = "New Elephant",
        item_price = 100,
        item_amount = 100, // must be item_price*quantity.value,
        item_code = "ru.some.elephant",
        tax_type = 6,
        quantity = Quantity(
            1,
            "thing")
    )
    val orderInfo = OrderInfo(
        order_id = randomUUID().toString(),
        order_number = "1",
        description = "Покупка слона",
        tax_system = 0,
        amount = cardInfo.item_amount,
        purpose = "OOO Elephant Seller",
        service_id = "27"
    )
<...>
```

## Создание счета

Отправьте сформированную корзину в сценарий с помощью библиотеки **messaging** — придумайте идентификатор, например, `ACTION_FROM_NATIVE_APP`, и отправьте его в сообщении `SERVER_ACTION`.

Пример из [демо-приложения](https://github.com/salute-developers/native-sdk/tree/main/demo):

```
fun addItemsToCartAndPay() {
<...>
    viewModelScope.launch(ioCoroutineDispatcher) {
        messaging.sendAction(
            MessageName.SERVER_ACTION,
            formBuyServerActionPayload(
                cardInfo = cardInfo,
                orderInfo = orderInfo
            )
        )
    }
}
<...>
private fun formBuyServerActionPayload(cardInfo: CardInfo, orderInfo: OrderInfo): Payload =
    Payload(
        json.encodeToString(
            ServerAction(
                actionId = "ACTION_FROM_NATIVE_APP",
                parameters = BuyParameters(
                    cardInfo = cardInfo,
                    orderInfo = orderInfo
                )
            )
        )
    )
```

Настройте в **main.sc** сценария обработку действия `ACTION_FROM_NATIVE_APP`. Используя данные о корзине, создайте счет.

Пример из сценария:

```
    state: ActionNativeApp
        event!: ACTION_FROM_NATIVE_APP
        script:
            var request = $jsapi.context().request;
            $jsapi.log("REQUEST: " + JSON.stringify(request.data.eventData));
             
            $payment.clearItems();
            $payment.addItem(request.data.eventData.cardInfo);
             
            var orderInfo = { order: request.data.eventData.orderInfo };
            $jsapi.log("ORDER INFO " + JSON.stringify(orderInfo))
             
            var response = $payment.createPayment(orderInfo);
            $jsapi.log("Response: " + JSON.stringify(response))
<...>
```


## Запуск оплаты

После создания счета вы получите его идентификатор — `invoice_id`. Сохраните его в сценарии:

```
    state: ActionNativeApp
        event!: ACTION_FROM_NATIVE_APP
        script:
<...>           
            $session.invoice_id = response.invoice_id;
            $reactions.pay($session.invoice_id);
```

Команда `$reactions.pay` отправляет на устройство сообщение типа `POLICY_RUN_APP` для запуска библиотеки оплаты. Пользователю отображается диалог об оплате.

## Проведение оплаты
После завершения диалога об оплате необходимо получить его статус. Для этого в **main.cs** сценария добавьте новый state, который принимает команды с уникальным идентификатором `PAY_DIALOG_FINISHED`.

Результат работы диалога оплаты отображается в параметре response_code. Возможные значения `response_code` смотрите в разделе [Формат результата оплаты](https://developers.sber.ru/docs/ru/va/how-to/monetization/payments/payment-processing).

```
state: PayDialogFinished
    event!: PAY_DIALOG_FINISHED
    script:
        try {
            $jsapi.log("| Sample | PAY_DIALOG_FINISHED: " + toPrettyString($request.data.eventData.payment_response));
            $temp.code = $request.data.eventData.payment_response.response_code;
            $jsapi.log("check invoice: response_code = "+$temp.code + "; device = "+$session.surface);           
            $reactions.transition("/ShowPaymentStatus");
        } catch(e) {
            $jsapi.log("catch(e)" + e.message);
            $reactions.transition("/PaymentStatusError");
        }
```        

## Получение статуса платежа

Для проверки статуса платежа используйте запрос [GET /invoices/{invoice_id}](https://developers.sber.ru/docs/ru/va/reference/smartservices/smartpay/processing/smartpay-api).

Убедитесь, что заказ перешел в [финальный статус](https://developers.sber.ru/docs/ru/va/reference/smartservices/smartpay/processing/payment-statuses), и отобразите результат пользователю.


