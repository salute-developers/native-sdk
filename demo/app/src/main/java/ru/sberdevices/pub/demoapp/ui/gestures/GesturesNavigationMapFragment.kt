package ru.sberdevices.sdk.demoapp.ui.gestures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.sberdevices.common.logger.Logger
import ru.sberdevices.sdk.demoapp.ui.gestures.controller.GridAdapter
import ru.sberdevices.services.pub.demoapp.R
import ru.sberdevices.services.pub.demoapp.databinding.FragmentGesturesNavigationMapBinding

class GesturesNavigationMapFragment : Fragment(R.layout.fragment_gestures_navigation_map) {

    private val logger = Logger.get("GesturesNavigationMapFragment")

    private val viewModel: GesturesNavigationMapViewModel by viewModel()
    private lateinit var viewBinding: FragmentGesturesNavigationMapBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentGesturesNavigationMapBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        logger.debug { "onViewCreated()" }
        val gridAdapter = GridAdapter(requireContext())
        with(viewBinding.gridView) {
            numColumns = viewModel.gridController.gridSize
            adapter = gridAdapter
        }

        viewModel.gridController.colorTile(gridAdapter, 1, 2)

        viewModel.gesturesFlow.onEach {
            logger.debug { "onNewGesture: $it" }
            viewModel.gridController.moveTile(gridAdapter, it)
        }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, minActiveState = Lifecycle.State.STARTED)
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    companion object {
        fun newInstance() = GesturesNavigationMapFragment()
    }
}
