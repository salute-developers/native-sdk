package ru.sberdevices.pub.demoapp.ui.tabscreen.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.sberdevices.common.logger.Logger

/**
 * View model for main tabs fragment
 */
class TabsViewModel : ViewModel() {

    private val logger = Logger.get("TabsViewModel")
    private val _isCameraAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCameraAvailable = _isCameraAvailable.asStateFlow()
    
    init {
        logger.debug { "init(), model: ${Build.MODEL}" }
        _isCameraAvailable.tryEmit(Build.MODEL in DEVICES_WITH_CAMERA)
    }

    companion object {
        val DEVICES_WITH_CAMERA = listOf("SberPortal", "SberBox Top")
    }
}