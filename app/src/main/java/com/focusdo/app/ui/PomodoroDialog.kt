package com.focusdo.app.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.focusdo.app.R
import com.focusdo.app.databinding.DialogPomodoroBinding
import com.focusdo.app.util.DateUtils
import kotlinx.coroutines.launch

class PomodoroDialog : DialogFragment() {

    companion object {
        fun newInstance() = PomodoroDialog()
    }

    private var _binding: DialogPomodoroBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_FocusDo_PomodoroDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogPomodoroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buddyView.setOnDark(true)

        binding.btnClose.setOnClickListener {
            viewModel.stopFocus()
            dismiss()
        }
        binding.btnPause.setOnClickListener { viewModel.togglePause() }
        binding.btnStop.setOnClickListener {
            viewModel.stopFocus()
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.focus.collect { state ->
                    if (!state.active) {
                        dismiss()
                        return@collect
                    }

                    val task = viewModel.allTasks.value.find { it.id == state.taskId }
                    binding.tvTaskTitle.text = task?.title ?: ""
                    binding.tvSessionCount.text = "오늘 ${state.sessionCount}번째 집중"

                    // Tomato display
                    val toms = task?.tomatoes ?: 0
                    binding.tvTomatoes.text = if (toms > 8) "🍅 ×$toms" else "🍅".repeat(toms)

                    // Timer
                    val pomSec = viewModel.pomodoroDuration.value * 60
                    val remaining = pomSec - state.cycleElapsed
                    binding.tvTimer.text = DateUtils.formatTimer(remaining)

                    // Ring progress: 1.0 → full (just started), 0.0 → finished
                    val progress = if (pomSec > 0) remaining.toFloat() / pomSec else 0f
                    binding.timerRing.progress = progress

                    // Paused state
                    val paused = state.paused
                    binding.buddyView.setPaused(paused)
                    binding.btnPause.text = if (paused) "다시 시작" else "일시 정지"
                    binding.tvTimer.alpha = if (paused) 0.45f else 1f
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
