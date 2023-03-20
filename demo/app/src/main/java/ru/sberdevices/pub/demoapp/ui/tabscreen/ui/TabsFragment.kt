package ru.sberdevices.pub.demoapp.ui.tabscreen.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.pub.demoapp.ui.cv.ComputerVisionFragment
import ru.sberdevices.pub.demoapp.ui.environmentinfo.EnvironmentInfoFragment
import ru.sberdevices.pub.demoapp.ui.smartapp.ui.SmartAppFragment
import ru.sberdevices.pub.demoapp.ui.tabscreen.util.enterImmersiveMode
import ru.sberdevices.pub.demoapp.ui.tabscreen.util.exitImmersiveMode
import ru.sberdevices.sdk.demoapp.ui.gestures.GesturesNavigationMapFragment
import ru.sberdevices.services.pub.demoapp.R
import ru.sberdevices.services.pub.demoapp.databinding.FragmentTabsBinding

class TabsFragment : Fragment() {

    private val logger = Logger.get("TabsFragment")

    private val viewModel: TabsViewModel by viewModel()
    private lateinit var binding: FragmentTabsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setClickListeners()

        renderTabSelection(TabUi.ENVIRONMENT_INFO)

        lifecycleScope.launchWhenCreated {
            viewModel.isCameraAvailable.collect { hasCamera ->
                binding.cvTabButton.isEnabled = hasCamera
            }
        }
    }

    private fun setClickListeners() {
        binding.environmentInfoTabButton.setOnClickListener { renderTabFragment(TabUi.ENVIRONMENT_INFO) }
        binding.servicesTabButton.setOnClickListener { renderTabSelection(TabUi.SERVICES) }
        binding.cvTabButton.setOnClickListener { renderTabSelection(TabUi.CV) }
        binding.gesturesTabButton.setOnClickListener { renderTabSelection(TabUi.GESTURES) }
    }

    private fun renderTabSelection(selectedTab: TabUi) {
        logger.debug { "Render tab selection $selectedTab" }

        binding.environmentInfoTabButton.isSelected = selectedTab == TabUi.ENVIRONMENT_INFO
        binding.servicesTabButton.isSelected = selectedTab == TabUi.SERVICES
        binding.cvTabButton.isSelected = selectedTab == TabUi.CV

        renderTabFragment(selectedTab)
    }

    private fun showChildFragment(fragment: Fragment, @IdRes fragmentContainerViewId: Int) {
        childFragmentManager.beginTransaction().apply {
            replace(fragmentContainerViewId, fragment)
            commit()
        }
    }

    private fun renderTabFragment(tab: TabUi) {
        val fragment = when (tab) {
            TabUi.ENVIRONMENT_INFO -> {
                requireActivity().window.exitImmersiveMode()
                EnvironmentInfoFragment.newInstance()
            }
            TabUi.SERVICES -> {
                requireActivity().window.exitImmersiveMode()
                SmartAppFragment.newInstance()
            }
            TabUi.CV -> {
                requireActivity().window.enterImmersiveMode()
                ComputerVisionFragment.newInstance()
            }
            TabUi.GESTURES -> {
                requireActivity().window.exitImmersiveMode()
                GesturesNavigationMapFragment.newInstance()
            }
        }
        showChildFragment(fragment, R.id.fragmentContainerView)
    }

    companion object {
        enum class TabUi {
            /**
             * Tab for environment info.
             */
            ENVIRONMENT_INFO,

            /**
             * Tab for services demo
             */
            SERVICES,

            /**
             * Tab for Computer Vision demo
             */
            CV,

            /**
             * Tab of gestures navigation demo
             */
            GESTURES
        }
    }
}
