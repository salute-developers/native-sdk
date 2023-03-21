package ru.sberdevices.common.binderhelper.sdk

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.sberdevices.common.binderhelper.BinderHelper
import ru.sberdevices.common.binderhelper.SinceVersion
import ru.sberdevices.common.binderhelper.VersionNotFoundException

/**
 * Тесты для [VersionedServiceSdkProxy]
 * @author Максим Митюшкин on 14.03.2023
 */
internal class VersionedServiceSdkProxyTest {

    private lateinit var spyTestService: TestService
    private lateinit var testServiceImpl: TestService
    private lateinit var binderHelper: BinderHelper<*>

    @Before
    fun setUp() {
        spyTestService = mockk(relaxed = true)
        every { spyTestService.property } returns 2023
        every { spyTestService.versionedProperty } returns 2023
        every { spyTestService.method() } returns 2023
        every { spyTestService.versionedMethod() } returns 2023
        coEvery { spyTestService.suspendMethod() } returns 2023
        coEvery { spyTestService.versionedSuspendMethod() } returns 2023
        every { spyTestService.versionedResultMethod() } returns Result.success(2023)

        testServiceImpl = TestServiceImpl(spyTestService)
        binderHelper = mockk(relaxed = true)
    }

    @Test
    fun `Если у проперти нет аннотации @SinceVersion, то вызов просто проксируется в impl`() {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к проперти и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.property)
        verify(exactly = 1) { spyTestService.property }
    }

    @Test
    fun `Если у метода нет аннотации @SinceVersion, то вызов просто проксируется в impl`() {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к методу и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.method())
        verify(exactly = 1) { spyTestService.method() }
    }

    @Test
    fun `Если у suspend-метода нет аннотации @SinceVersion, то вызов просто проксируется в impl`() = runBlockingTest {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к методу и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.suspendMethod())
        coVerify(exactly = 1) { spyTestService.suspendMethod() }
    }

    @Test
    fun `Если у проперти есть аннотация @SinceVersion и версии совместимы, то вызов будет проксирован`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true

        // Обратимся к проперти и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.versionedProperty)
        verify(exactly = 1) { spyTestService.versionedProperty }
    }

    @Test
    fun `Если у метода есть аннотация @SinceVersion и версии совместимы, то вызов будет проксирован`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true

        // Обратимся к проперти и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.versionedMethod())
        verify(exactly = 1) { spyTestService.versionedMethod() }
    }

    @Test
    fun `Если у suspend-метода есть аннотация @SinceVersion и версии совместимы, то вызов будет проксирован`() =
        runBlockingTest {
            every { binderHelper.getVersion() } returns Result.success(100)
            every { binderHelper.hasService() } returns true

            // Обратимся к проперти и посмотрим, был ли проксирован вызов.
            val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
            assertEquals(2023, proxy.versionedSuspendMethod())
            coVerify(exactly = 1) { spyTestService.versionedSuspendMethod() }
        }

    @Test(expected = IncompatibilityVersionException::class)
    fun `Если у проперти есть аннотация @SinceVersion и версии не совместимы, то вылетит IncompatibilityVersionException`() {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к проперти и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        proxy.versionedProperty
    }

    @Test(expected = IncompatibilityVersionException::class)
    fun `Если у метода есть аннотация @SinceVersion и версии не совместимы, то вылетит IncompatibilityVersionException`() {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к проперти и посмотрим, был ли проксирован вызов.
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        proxy.versionedMethod()
    }

    @Test(expected = IncompatibilityVersionException::class)
    @Suppress("MaxLineLength")
    fun `Если у suspend-метода есть аннотация @SinceVersion и версии не совместимы, то вылетит IncompatibilityVersionException`() =
        runBlockingTest {
            every { binderHelper.getVersion() } returns Result.success(99)
            every { binderHelper.hasService() } returns true

            // Обратимся к проперти и посмотрим, был ли проксирован вызов.
            val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
            proxy.versionedSuspendMethod()
        }

    @Test
    fun `Если метод возвращает Result, то IncompatibilityVersionException будет упакован в Failure`() {
        every { binderHelper.getVersion() } returns Result.success(99)
        every { binderHelper.hasService() } returns true

        // Обратимся к методу и посмотрим что он вернет
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertTrue(proxy.versionedResultMethod().exceptionOrNull() is IncompatibilityVersionException)
    }

    @Test
    fun `Если метод возвращает Result, то успешный результат метода реализации будет будет упакован в Success`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true

        // Обратимся к методу и посмотрим что он вернет
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.versionedResultMethod().getOrNull())
    }

    @Test
    fun `Если дефолтный метод не переопределен в реализации, то он спокойно вызывается`() {
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.defaultMethod())
    }

    @Test
    fun `Повторное обращение к элементам реализации отрабатывает в штатном режиме`() = runBlockingTest {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)

        repeat(2) {
            assertEquals(2023, proxy.property)
            assertEquals(2023, proxy.method())
            assertEquals(2023, proxy.defaultMethod())
            assertEquals(2023, proxy.suspendMethod())
            assertEquals(2023, proxy.versionedResultMethod().getOrNull())
            assertEquals(2023, proxy.versionedProperty)
            assertEquals(2023, proxy.versionedMethod())
            assertEquals(2023, proxy.versionedSuspendMethod())
        }
    }

    @Test
    fun `Методы toString(), hashCode() функционируют идентично реализации`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)

        assertEquals(testServiceImpl.toString(), proxy.toString())
        assertEquals(testServiceImpl.hashCode(), proxy.hashCode())
    }

    @Test
    @Suppress("UnusedEquals")
    fun `equals(), если не переопределен, не вызывает падений + выдает true, если сравнивает impl и прокси`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)

        // Выполним метод и убедимся, что падений нет.
        @Suppress("UnusedEquals")
        proxy.equals(1)
        // Сравнение работает только в одну сторону, т.к. testServiceImpl не реализует equals(),
        // а также, потому, что библиотека построена
        @Suppress("ReplaceCallWithBinaryOperator")
        assertTrue(proxy.equals(testServiceImpl))
    }

    @Test
    fun `Если у сервиса нет версии, то проверки не производятся и вызов проперти проксируется`() {
        every { binderHelper.hasService() } returns true
        every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.versionedProperty)
        verify(exactly = 1) { spyTestService.versionedProperty }
    }

    @Test
    fun `Если у сервиса нет версии, то проверки не производятся и вызов метода проксируется`() {
        every { binderHelper.hasService() } returns true
        every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.versionedMethod())
        verify(exactly = 1) { spyTestService.versionedMethod() }
    }

    @Test
    fun `Если у сервиса нет версии, то проверки не производятся и вызов suspend-метода проксируется`() =
        runBlockingTest {
            every { binderHelper.hasService() } returns true
            every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

            val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
            assertEquals(2023, proxy.versionedSuspendMethod())
            coVerify(exactly = 1) { spyTestService.versionedSuspendMethod() }
        }

    @Test(expected = IncompatibilityVersionException::class)
    fun `Если сервис в системе не существует, то выкидывается IncompatibilityVersionException`() {
        every { binderHelper.hasService() } returns false
        every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        proxy.versionedMethod()
    }

    @Test
    fun `Если сервис в системе не существует и на проперти нет аннотации, то производится его проксирование`() {
        every { binderHelper.hasService() } returns false
        every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.property)
        verify(exactly = 1) { spyTestService.property }
    }

    @Test
    fun `Если сервис в системе не существует и на методе нет аннотации, то производится его проксирование`() {
        every { binderHelper.hasService() } returns false
        every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals(2023, proxy.method())
        verify(exactly = 1) { spyTestService.method() }
    }

    @Test
    fun `Если сервис в системе не существует и на suspend-методе нет аннотации, то производится его проксирование`() =
        runBlockingTest {
            every { binderHelper.hasService() } returns false
            every { binderHelper.getVersion() } returns Result.failure(VersionNotFoundException("Version not found!"))

            val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
            assertEquals(2023, proxy.suspendMethod())
            coVerify(exactly = 1) { spyTestService.suspendMethod() }
        }

    @Test
    fun `Если реализация выкидывает ResultFailure, то не происходит двойной упаковки`() {
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)
        assertEquals("Mock exception!", proxy.resultFailureMethod().exceptionOrNull()?.message)
    }

    @Test
    fun `Для проксирования выбирается правильный метод, если он перегружен`() {
        every { binderHelper.getVersion() } returns Result.success(100)
        every { binderHelper.hasService() } returns true
        val proxy = VersionedServiceSdkProxy.proxy<TestService>(testServiceImpl, binderHelper)

        proxy.overloadedMethod()
        verify(exactly = 1) { spyTestService.overloadedMethod() }
        verify(exactly = 0) { spyTestService.overloadedMethod(any()) }
        verify(exactly = 0) { spyTestService.overloadedMethod(any(), any<Boolean>()) }
        verify(exactly = 0) { spyTestService.overloadedMethod(any(), any<Int>()) }

        proxy.overloadedMethod(1)
        verify(exactly = 1) { spyTestService.overloadedMethod() }
        verify(exactly = 1) { spyTestService.overloadedMethod(1) }
        verify(exactly = 0) { spyTestService.overloadedMethod(any(), any<Boolean>()) }
        verify(exactly = 0) { spyTestService.overloadedMethod(any(), any<Int>()) }

        proxy.overloadedMethod(1, false)
        verify(exactly = 1) { spyTestService.overloadedMethod() }
        verify(exactly = 1) { spyTestService.overloadedMethod(any()) }
        verify(exactly = 1) { spyTestService.overloadedMethod(1, false) }
        verify(exactly = 0) { spyTestService.overloadedMethod(any(), any<Int>()) }

        proxy.overloadedMethod(true, 1)
        verify(exactly = 1) { spyTestService.overloadedMethod() }
        verify(exactly = 1) { spyTestService.overloadedMethod(any()) }
        verify(exactly = 1) { spyTestService.overloadedMethod(any(), any<Boolean>()) }
        verify(exactly = 1) { spyTestService.overloadedMethod(true, 1) }
    }

    private interface TestService {
        val property: Int
        fun method(): Int
        fun defaultMethod(): Int = 2023
        fun resultFailureMethod(): Result<Int>
        suspend fun suspendMethod(): Int

        fun versionedResultMethod(): Result<Int>
        val versionedProperty: Int
        fun versionedMethod(): Int
        suspend fun versionedSuspendMethod(): Int

        fun overloadedMethod(): Int
        fun overloadedMethod(a: Int): Int
        fun overloadedMethod(a: Int, b: Boolean): Int
        fun overloadedMethod(a: Boolean, b: Int): Int
    }

    private class TestServiceImpl(private val spyObject: TestService) : TestService {
        override val property: Int = spyObject.property
        override fun method(): Int = spyObject.method()
        override fun resultFailureMethod(): Result<Int> = Result.failure(Exception("Mock exception!"))
        override fun overloadedMethod(): Int = spyObject.overloadedMethod()
        override fun overloadedMethod(a: Int): Int = spyObject.overloadedMethod(a)
        override fun overloadedMethod(a: Int, b: Boolean): Int = spyObject.overloadedMethod(a, b)
        override fun overloadedMethod(a: Boolean, b: Int): Int = spyObject.overloadedMethod(a, b)

        override suspend fun suspendMethod(): Int = spyObject.suspendMethod()

        @SinceVersion(100)
        override val versionedProperty: Int = spyObject.versionedProperty

        @SinceVersion(100)
        override fun versionedMethod(): Int = spyObject.versionedMethod()

        @SinceVersion(100)
        override fun versionedResultMethod(): Result<Int> = spyObject.versionedResultMethod()

        @SinceVersion(100)
        override suspend fun versionedSuspendMethod(): Int = spyObject.versionedSuspendMethod()
    }
}
