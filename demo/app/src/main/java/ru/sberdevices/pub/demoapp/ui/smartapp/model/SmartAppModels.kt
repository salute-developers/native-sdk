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
