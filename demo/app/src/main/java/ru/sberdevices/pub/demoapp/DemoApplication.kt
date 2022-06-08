package ru.sberdevices.pub.demoapp

import android.app.Application
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import ru.sberdevices.assistant.PublicAssistantFactory
import ru.sberdevices.common.assert.Asserts
import ru.sberdevices.common.binderhelper.BinderHelperFactory2
import ru.sberdevices.common.binderhelper.BinderHelperFactory2Impl
import ru.sberdevices.common.coroutines.CoroutineDispatchers
import ru.sberdevices.common.logger.AndroidLoggerDelegate
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.cv.detection.CvApiFactory
import ru.sberdevices.cv.detection.CvApiFactoryImpl
import ru.sberdevices.messaging.MessagingFactory
import ru.sberdevices.pub.demoapp.repository.GesturesRepository
import ru.sberdevices.pub.demoapp.ui.cv.ComputerVisionViewModel
import ru.sberdevices.pub.demoapp.ui.smartapp.ui.SmartAppViewModel
import ru.sberdevices.pub.demoapp.ui.tabscreen.ui.TabsViewModel
import ru.sberdevices.sdk.demoapp.ui.gestures.GesturesNavigationMapViewModel
import ru.sberdevices.sdk.demoapp.ui.gestures.controller.GridController
import ru.sberdevices.services.appstate.AppStateManagerFactory
import ru.sberdevices.services.paylib.PayLibFactory
import ru.sberdevices.services.pub.demoapp.BuildConfig

class DemoApplication : Application() {

    private val logger = Logger.get("SdkDemoApplication")

    init {
        Asserts.enabled = BuildConfig.DEBUG
        Logger.setDelegates(AndroidLoggerDelegate())
    }

    private val utilsModule = module {
        single { AppStateManagerFactory.createHolder(context = get()) }
        single<BinderHelperFactory2> { BinderHelperFactory2Impl() }
        single { GesturesRepository() }
    }

    private val viewModelModule = module {
        single { SmartAppViewModel(
            messaging = get(),
            appStateHolder = get(),
            ioCoroutineDispatcher = Dispatchers.IO,
            paylib = get(),
            assistant = get()
        )
        }
        viewModel {
            ComputerVisionViewModel(
                cvApiFactory = get(),
                ioCoroutineDispatcher = Dispatchers.IO
            )
        }
        viewModel {
            GesturesNavigationMapViewModel(
                gridController = GridController(),
                gesturesRepository = get()
            )
        }
        viewModel { TabsViewModel() }
    }

    private val sdkModule = module {
        factory<CvApiFactory> { CvApiFactoryImpl(this@DemoApplication) }
        factory {
            MessagingFactory.create(appContext = get())
        }
        factory {
            PayLibFactory(
                context = androidContext(),
                coroutineDispatchers = CoroutineDispatchers,
                binderHelperFactory2 = get()
            ).create()
        }
        factory {
            PublicAssistantFactory(
                context = androidContext(),
                coroutineDispatchers = CoroutineDispatchers,
                binderHelperFactory2 = get()
            ).create()
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger.debug { "onCreate" }

        // Example of getting device's info
        logger.info { "Running on device: ${Build.BRAND} ${Build.MODEL}" }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            initApp()
        }
    }

    private fun initApp() {
        startKoin {
            androidContext(this@DemoApplication)
            modules(listOf(utilsModule, viewModelModule, sdkModule))
        }
    }
}
