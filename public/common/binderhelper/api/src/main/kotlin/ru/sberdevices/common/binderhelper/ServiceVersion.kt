package ru.sberdevices.common.binderhelper

/**
 * Важно! в аидл файле должна быть весия (поле Version: Int),
 * и в андроид-сервисе, в метадатае также должна
 * быть версия (поле ru.sberdevices.services.version.key), и они должны совпадать
 */
typealias ServiceVersion = Int

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY)
/**
 * Аннотация отмечает поля сдк-классов, который доступны начиная с определенной версии
 * аидл сервиса.
 * StarPlatformServices обновляется не синхронно с клиентскими приложениями,
 * поэтому версия доступного сервиса и аидл интерфейсом клиента могут не совпадать,
 */
annotation class SinceVersion(
    val version: ServiceVersion
)

class VersionNotFoundException(description: String) : RuntimeException(description)

const val SERVICE_VERSION_KEY = "ru.sberdevices.services.version.key"

interface Versional {
    /**
     * Отдает версию сервиса, к которому пытается соединится=
     * @return
     * [Result.success] содержит [ServiceVersion] - версия сервиса
     * [Result.failure] содержит [VersionNotFoundException] с описанием почему версия недоступна
     */
    fun getVersion(): Result<ServiceVersion>
}
