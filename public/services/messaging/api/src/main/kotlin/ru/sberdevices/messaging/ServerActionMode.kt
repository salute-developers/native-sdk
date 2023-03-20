package ru.sberdevices.messaging

/**
 * Опциональное поле для ServerAction. Необходимо для фильтрации на бекенде.
 * Для фильтрации данных используется "mode" = "background", явно указывающий на то,
 * что server_action не может повлиять на состояние UI-я AssistantSDK (фильтруется озвучка,
 * саджесты и возможность открыть новый экран)
 */
enum class ServerActionMode {
    BACKGROUND,
    FOREGROUND
}