package ru.sberdevices.pub.demoapp.ui.environmentinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.sberdevices.services.pub.demoapp.databinding.FragmentEnvironmentInfoBinding

class EnvironmentInfoFragment : Fragment() {

    private lateinit var viewBinding: FragmentEnvironmentInfoBinding
    private val viewModel by viewModel<EnvironmentInfoViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentEnvironmentInfoBinding.inflate(layoutInflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            viewModel.starOsVersionFlow.collect { viewBinding.environmentInfoVersionText.text = it ?: "old os" }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.deviceTypeFlow.collect { viewBinding.environmentInfoDeviceTypeText.text = it?.name ?: "old os" }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.screenStateFlow.collect {
                viewBinding.environmentInfoDreamStateText.text = it?.dreamState?.name ?: "old os"
                viewBinding.environmentInfoNoScreenModeEnabledText.text =
                    it?.isNoScreenModeEnabled?.toString() ?: "old os"
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.userSettingsInfo.collect {
                viewBinding.environmentInfoChildModeEnabledText.text = it?.isChildModeEnabled?.toString() ?: "old os"
                viewBinding.environmentInfoDeviceLockModeText.text = it?.deviceLockMode?.name ?: "old os"
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.cameraStateFlow.collect {
                viewBinding.cameraStateText.text = it?.name ?: "old os"
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.micStateFlow.collect {
                viewBinding.microphoneStateText.text = it?.name ?: "old os"
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.isCameraCovered.collect {
                viewBinding.cameraCoveredStateText.text = "${it ?: "old os"}"
            }
        }

        viewBinding.assistantServiceText.text = "${viewModel.assistantServiceVersion ?: "unsupported"}"
        viewBinding.micCameraStateServiceText.text = "${viewModel.micCameraStateServiceVersion ?: "unsupported"}"
        viewBinding.payLibServiceText.text = "${viewModel.paylibServiceVersion ?: "unsupported"}"
        viewBinding.messagingServiceText.text = "${viewModel.messagingServiceVersion ?: "unsupported"}"
        viewBinding.envInfoServiceText.text = "${viewModel.environmentInfoServiceVersion ?: "unsupported"}"
    }

    companion object {
        fun newInstance() = EnvironmentInfoFragment()
    }
}
