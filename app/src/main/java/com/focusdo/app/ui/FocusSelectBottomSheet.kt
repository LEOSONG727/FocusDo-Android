package com.focusdo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.focusdo.app.R
import com.focusdo.app.databinding.BottomSheetFocusSelectBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FocusSelectBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFocusSelectBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetFocusSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val incomplete = viewModel.allTasks.value.filter { !it.completed }

        if (incomplete.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.taskContainer.visibility = View.GONE
            return
        }

        binding.tvEmpty.visibility = View.GONE
        binding.taskContainer.visibility = View.VISIBLE
        binding.taskContainer.removeAllViews()

        incomplete.forEach { task ->
            val tv = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_focus_select, binding.taskContainer, false) as TextView
            tv.text = task.title
            tv.setOnClickListener {
                viewModel.startFocus(task.id)
                dismiss()
                PomodoroDialog.newInstance().show(parentFragmentManager, "pomodoro")
            }
            binding.taskContainer.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
