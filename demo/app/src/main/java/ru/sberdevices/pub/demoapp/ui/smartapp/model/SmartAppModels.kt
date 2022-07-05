package ru.sberdevices.pub.demoapp.ui.smartapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Items variants
 */
@Serializable
enum class Clothes(val clothes: String) {
    @SerialName("шапку")
    BEANIE("шапка"),

    @SerialName("перчатки")
    GLOVES("перчатки"),

    @SerialName("ботинки")
    BOOTS("ботинки"),

    @SerialName("куртку")
    JACKET("куртка")
}

@Serializable
enum class NavCommand() {

    /**
     * Фразы, которые ассистент обрабатывает как "UP":
     * Вверх, Прокрути вверх, Выше, фразы из корзины
     */
    @SerialName("UP")
    UP,

    /**
     * Фразы, которые ассистент обрабатывает как "DOWN":
     * Вниз, Прокрути вниз, Опусти, Ниже
     */
    @SerialName("DOWN")
    DOWN,

    /**
     * Фразы, которые ассистент обрабатывает как "LEFT"
     * Влево, В лево, Налево, На лево
     */
    @SerialName("LEFT")
    LEFT,

    /**
     * Фразы, которые ассистент обрабатывает как "RIGHT"
     *
     * Список фраз:
     * Вправо, В право, Направо, На право
     */
    @SerialName("RIGHT")
    RIGHT,

    /**
     * Фразы, которые ассистент обрабатывает как "FORWARD"
     *
     * Список фраз:
     * Вперед, Дальше, Далее, Следующая, Следующая страницы, Покажи следующую, Покажи еще
     */
    @SerialName("FORWARD")
    FORWARD
}

@Serializable
class NavigationCommand(
    @SerialName("command")
    val command: NavCommand
)

/**
 * Base command from smartapp backend
 */
@Serializable
sealed class BaseCommand

/**
 * Command for dressing up the Android
 */
@Serializable
@SerialName("wear_this")
internal class WearThisCommand(
    val clothes: Clothes? = null
): BaseCommand()

/**
 * Command for undressing the Android
 */
@Serializable
@SerialName("dont_wear_anything")
internal class ClearClothesCommand: BaseCommand()

/**
 * Items for purchase
 */
@Serializable
enum class BuyItems {
    @SerialName("elephant")
    ELEPHANT
}

/**
 * JSON [myState] state that is pulled to smartapp backend
 */
@Serializable
internal data class MyAppState(
    val myState: String
)

/**
 * Send intent to do some action on smart app backend. It can be some event in game, or some input from user.
 * Event can be caught on server side by its [actionId] and carry some useful payload in its [parameters]
 */
@Serializable
internal data class ServerAction<T>(
    @SerialName("action_id")
    val actionId: String,

    @SerialName("parameters")
    val parameters: T
)
