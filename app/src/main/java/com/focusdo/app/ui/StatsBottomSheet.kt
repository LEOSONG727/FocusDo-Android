package com.focusdo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.focusdo.app.databinding.BottomSheetStatsBinding
import com.focusdo.app.util.DateUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class StatsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { s ->
                    binding.tvTomatoes.text = "🍅 × ${s.dayTomatoes}"
                    binding.tvDayTime.text = DateUtils.formatFocusTime(s.dayTime)
                    binding.tvWeekTime.text = DateUtils.formatFocusTime(s.weekTime)
                    binding.tvMonthTime.text = DateUtils.formatFocusTime(s.monthTime)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
