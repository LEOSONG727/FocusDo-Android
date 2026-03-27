package com.focusdo.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.focusdo.app.data.Task
import com.focusdo.app.databinding.BottomSheetTaskEditBinding
import com.focusdo.app.util.DateUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class TaskEditBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TASK_ID    = "task_id"
        private const val ARG_TASK_TITLE = "task_title"
        private const val ARG_TASK_DATE  = "task_date"
        private const val ARG_INIT_DATE  = "init_date"
        private const val ARG_AUTO_START = "auto_start"

        fun newTask(initDate: String, autoStart: Boolean = false) =
            TaskEditBottomSheet().apply {
                arguments = bundleOf(ARG_INIT_DATE to initDate, ARG_AUTO_START to autoStart)
            }

        fun editTask(task: Task) =
            TaskEditBottomSheet().apply {
                arguments = bundleOf(
                    ARG_TASK_ID    to task.id,
                    ARG_TASK_TITLE to task.title,
                    ARG_TASK_DATE  to task.date
                )
            }
    }

    private var _binding: BottomSheetTaskEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private var selectedDate = DateUtils.today()
    private var editingTaskId: Long? = null
    private var autoStart = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTaskEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: Bundle()
        editingTaskId = if (args.containsKey(ARG_TASK_ID)) args.getLong(ARG_TASK_ID) else null
        autoStart = args.getBoolean(ARG_AUTO_START, false)

        if (editingTaskId != null) {
            binding.tvSheetTitle.text = "작업 수정"
            binding.etTitle.setText(args.getString(ARG_TASK_TITLE, ""))
            selectedDate = args.getString(ARG_TASK_DATE, DateUtils.today())
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnStartFocus.visibility = View.GONE
        } else {
            binding.tvSheetTitle.text = "새 작업"
            selectedDate = args.getString(ARG_INIT_DATE, DateUtils.today())
            binding.btnDelete.visibility = View.GONE
            binding.btnStartFocus.visibility = View.VISIBLE
        }

        updateDateLabel()

        binding.btnDatePick.setOnClickListener { showDatePicker() }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnDelete.setOnClickListener {
            val id = editingTaskId ?: return@setOnClickListener
            viewModel.allTasks.value.find { it.id == id }?.let { viewModel.deleteTask(it) }
            dismiss()
        }

        binding.btnSave.setOnClickListener { submitTask(false) }
        binding.btnStartFocus.setOnClickListener { submitTask(true) }
    }

    private fun submitTask(startFocus: Boolean) {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            binding.tilTitle.error = "제목을 입력해주세요"
            return
        }
        binding.tilTitle.error = null

        val id = editingTaskId
        if (id != null) {
            val task = viewModel.allTasks.value.find { it.id == id }
            task?.let { viewModel.updateTask(it.copy(title = title, date = selectedDate)) }
        } else {
            val newId = viewModel.addTask(title, selectedDate)
            if (startFocus || autoStart) {
                dismiss()
                viewModel.startFocus(newId)
                PomodoroDialog.newInstance().show(parentFragmentManager, "pomodoro")
                return
            }
        }
        dismiss()
    }

    private fun showDatePicker() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val initialMs = runCatching { fmt.parse(selectedDate)?.time }.getOrNull()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("날짜 선택")
            .setSelection(initialMs)
            .build()

        picker.addOnPositiveButtonClickListener { ms ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = ms
            selectedDate = "%04d-%02d-%02d".format(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            updateDateLabel()
        }
        picker.show(parentFragmentManager, "datePicker")
    }

    private fun updateDateLabel() {
        val today = DateUtils.today()
        binding.btnDatePick.text = if (selectedDate == today) "오늘 ($selectedDate)" else selectedDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
