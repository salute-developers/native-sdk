package ru.sberdevices.common.binderhelper.sdk

import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.VersionNotFoundException
import ru.sberdevices.common.logger.Logger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.jvm.jvmErasure

/**
 * Класс, проксирующий вызовы к реализации SDK.
 * Осуществляет проверку совместимости вызываемого метода с текущей версией нужного сервиса
 * за счет ее сверки с минимальной версией, указанной с помощью аннотации [SinceVersion].
 * @param implInstance Инстанс реализации, в которую будем проксировать вызовы.
 * @param binderHelper BinderHelper, отвечающий за взаимодействие с сервисом, нужным для реализации.
 * @author Максим Митюшкин on 13.03.2023.
 */
class VersionedServiceSdkProxy(
    private val implInstance: Any,
    private val binderHelper: BinderHelper<*>,
) : InvocationHandler {

    private val logger = Logger.get("VersionedServiceSdkProxy<${implInstance.javaClass.name}>")
    private val kotlinCallables = ConcurrentHashMap<Method, Pair<KFunction<*>?, SinceVersion?>>()

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        logger.debug { "invoke(${method.name})" }

        val (kotlinCallable, sinceVersionAnnotation) = findKotlinCallable(method).apply { first?.isAccessible = true }
        if (kotlinCallable == null) {
            logger.error { "invoke(${method.name}): kotlin function/property not found!" }
            throw UnsupportedOperationException("Supported only Kotlin functions or properties!")
        }

        if (sinceVersionAnnotation != null) {
            logger.debug { "invoke(${method.name}), found @SinceVersion annotation" }

            if (!binderHelper.hasService()) {
                logger.error { "invoke(${method.name}): service not found" }
                throw IncompatibilityVersionException("Service not found!")
            }

            try {
                val serviceVersion = binderHelper.getVersion().getOrThrow()
                if (serviceVersion < sinceVersionAnnotation.version) {
                    logger.warn { "invoke(${method.name}), $serviceVersion < $sinceVersionAnnotation" }
                    return throwOrReturnException(
                        callable = kotlinCallable,
                        exception = IncompatibilityVersionException(
                            "Service version too low: " +
                                "required = ${sinceVersionAnnotation.version}, " +
                                "current = $serviceVersion"
                        )
                    )
                }

                logger.debug { "invoke(${method.name}), $serviceVersion >= ${sinceVersionAnnotation.version}" }
            } catch (exception: VersionNotFoundException) {
                logger.warn(exception) { "invoke(${method.name}), version not found, ignore." }
            }
        }

        val returnValue = if (args == null) {
            kotlinCallable.call(implInstance)
        } else {
            @Suppress("SpreadOperator")
            kotlinCallable.call(implInstance, *args)
        }

        // TODO В будущем идеально было бы научить работать с value-классами в целом.
        // Про проблему с двойной упаковкой value-классов см. комментарий в методе throwOrReturnException.
        return if (returnValue is Result<*>) {
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
            returnValue.value
        } else {
            returnValue
        }
    }

    private fun throwOrReturnException(callable: KFunction<*>, exception: Exception): Any? {
        if (isMethodReturnResult(callable)) {
            // Избегаем двойной упаковки. Компилятор Kotlin'а при своей работе везде заменяет value-классы на тип,
            // который лежит внутри него. Он также добавляет обратную запаковку значений в value class, когда мы
            // получаем его в качестве результата работы функции/при обращении к переменной.
            // Все было бы хорошо, но Dynamic Proxy не знает о таком виде классов и возвращает вместо внутреннего
            // значения внутри его инстанса сам инстанс value-класса, а Kotlin уже в свою очередь запаковывает его
            // обратно при получении, в итоге получаем двойную упаковку по типу Result(Result(11)), что не есть хорошо.
            // Лечится ручным возвратом внутреннего значения внутри инстанса.
            @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
            return Result.failure<Any>(exception).value
        }

        throw exception
    }

    @Suppress("ReturnCount")
    private fun findKotlinCallable(method: Method): Pair<KFunction<*>?, SinceVersion?> {
        logger.debug { "findKotlinFunction(${method.name})" }

        return kotlinCallables.getOrPut(method) {
            // Для начала попробуем поискать проперти и выдать соответствующий Java-методу
            // Kotlin-getter или setter в виде Kotlin-функции. Также найдем соответствующую аннотацию @SinceVersion по
            // следующему правилу: сначала проверим, не навешана ли она на сам геттер/сеттер, если нет, то тогда
            // посмотрим, не навешена ли она на проперти.
            implInstance::class.memberProperties.forEach {
                if (isMethodSignaturesEquals(it.javaGetter, method)) {
                    logger.debug { "findKotlinFunction(${method.name}): found kotlin-getter" }
                    return@getOrPut it.getter to (it.getter.findAnnotation() ?: it.findAnnotation())
                } else if (it is KMutableProperty<*> && isMethodSignaturesEquals(it.javaSetter, method)) {
                    logger.debug { "findKotlinFunction(${method.name}): found kotlin-setter" }
                    return@getOrPut it.setter to (it.setter.findAnnotation() ?: it.findAnnotation())
                }
            }

            // Если же не удалось найти нужный геттер или сеттер, то значит запрашивали не проперти, а метод.
            // Тогда приступим к поиску Kotlin-функции, соответствующей запрашиваемому Java-методу.
            val function = implInstance::class.memberFunctions.find { isMethodSignaturesEquals(it.javaMethod, method) }
                ?: return@getOrPut null to null
            return@getOrPut function to function.findAnnotation()
        }
    }

    private fun isMethodReturnResult(callable: KFunction<*>) =
        callable.returnType.jvmErasure.isSubclassOf(Result::class)

    private fun isMethodSignaturesEquals(first: Method?, second: Method?): Boolean {
        return first == second || (first != null
            && second != null
            && first.name == second.name
            && first.parameterTypes.contentEquals(second.parameterTypes))
    }

    companion object {
        /**
         * Создает инстанс переданного интерфейса, который будет проксировать вызовы
         * в оригинальную реализацию, если все проверки/действия будут выполнены успешно.
         * @param implInstance Инстанс реализации, в которую будем проксировать вызовы.
         * @param binderHelper BinderHelper, отвечающий за взаимодействие с сервисом, нужным для реализации.
         * @return Инстанс прокси, реализующий переданный интерфейс.
         */
        inline fun <reified Interface : Any> proxy(implInstance: Any, binderHelper: BinderHelper<*>): Interface {
            return Proxy.newProxyInstance(
                Interface::class.java.classLoader,
                arrayOf(Interface::class.java),
                VersionedServiceSdkProxy(implInstance, binderHelper)
            ) as Interface
        }
    }
}
