package com.focusdo.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.focusdo.app.R
import com.focusdo.app.databinding.ActivityMainBinding
import com.focusdo.app.util.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var taskAdapter: TaskAdapter

    // Notification permission launcher (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — timer still works, just no notification */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show onboarding on first launch
        val prefs = getSharedPreferences("focusdo_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_done", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Ask for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerRow) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = bars.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = bars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_bar_padding_v))
            insets
        }

        setupCalendar()
        setupTaskList()
        setupButtons()
        setupFilterChips()
        observeState()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { date -> viewModel.selectDate(date) }
        binding.rvCalendar.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
        }
    }

    private fun setupTaskList() {
        taskAdapter = TaskAdapter(
            onToggle     = { viewModel.toggleComplete(it) },
            onEdit       = { TaskEditBottomSheet.editTask(it).show(supportFragmentManager, "edit") },
            onStartFocus = { task ->
                viewModel.startFocus(task.id)
                PomodoroDialog.newInstance().show(supportFragmentManager, "pomodoro")
            }
        )
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        // Swipe-left to delete with visual red background + trash icon
        ItemTouchHelper(object : SwipeToDeleteCallback(this) {
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val task = taskAdapter.getTaskAt(vh.adapterPosition)
                viewModel.deleteTask(task)
                Snackbar.make(binding.root, "\"${task.title}\" 삭제됨", Snackbar.LENGTH_LONG)
                    .setAction("실행취소") { viewModel.updateTask(task) }
                    .show()
            }
        }).attachToRecyclerView(binding.rvTasks)
    }

    private fun setupButtons() {
        binding.btnPrevDate.setOnClickListener { viewModel.prevDate() }
        binding.btnNextDate.setOnClickListener { viewModel.nextDate() }

        binding.fabAddTask.setOnClickListener {
            TaskEditBottomSheet.newTask(viewModel.selectedDate.value)
                .show(supportFragmentManager, "newTask")
        }

        binding.btnStartFocus.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            FocusSelectBottomSheet().show(supportFragmentManager, "focusSelect")
        }

        binding.btnStats.setOnClickListener {
            StatsBottomSheet().show(supportFragmentManager, "stats")
        }

        binding.btnSettings.setOnClickListener { showSettingsDialog() }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = if (R.id.chipCompleted in checkedIds) "completed" else "all"
            viewModel.setFilter(filter)
        }
    }

    // ── Observation ────────────────────────────────────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.selectedDate.collect { updateDateUI(it) } }
                launch { viewModel.visibleTasks.collect { updateTaskList(it) } }
                launch { viewModel.achievement.collect { updateAchievement(it) } }
                launch { viewModel.toast.collect { showSnackbar(it) } }
                launch { viewModel.stats.collect { updateMiniStats(it) } }
                launch {
                    viewModel.focus.collect { state ->
                        if (state.active && supportFragmentManager.findFragmentByTag("pomodoro") == null) {
                            PomodoroDialog.newInstance().show(supportFragmentManager, "pomodoro")
                        }
                    }
                }
            }
        }
    }

    private fun updateDateUI(date: String) {
        val today = DateUtils.today()
        binding.tvSelectedDate.text = if (date == today) "오늘" else date

        // Build 15-day window (-7 to +7)
        val dates = (-7..7).map { DateUtils.addDays(date, it) }
        calendarAdapter.submitDates(dates, date)

        // Scroll calendar to center (index 7 = selected)
        (binding.rvCalendar.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(7, binding.rvCalendar.width / 2 - 100)
    }

    private fun updateTaskList(tasks: List<com.focusdo.app.data.Task>) {
        taskAdapter.submitList(tasks)
        binding.emptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        binding.rvTasks.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateAchievement(percent: Int) {
        binding.tvAchievement.text = "$percent%"
        binding.progressAchievement.progress = percent
    }

    private fun updateMiniStats(stats: MainViewModel.Stats) {
        if (stats.dayTime > 0 || stats.dayTomatoes > 0) {
            val timeStr = DateUtils.formatFocusTime(stats.dayTime)
            binding.tvMiniStats.text = "오늘 ${timeStr} 집중 · 🍅 ${stats.dayTomatoes}개"
            binding.tvMiniStats.visibility = View.VISIBLE
        } else {
            binding.tvMiniStats.visibility = View.GONE
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // ── Settings Dialog ────────────────────────────────────────────────────────

    private fun showSettingsDialog() {
        val currentDur = viewModel.pomodoroDuration.value
        val options = arrayOf("25분 (기본)", "50분 (딥 포커스)")
        val checked = if (currentDur == 25) 0 else 1

        MaterialAlertDialogBuilder(this)
            .setTitle("뽀모도로 시간")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val duration = if (which == 0) 25 else 50
                viewModel.setPomodoroDuration(duration)
                showSnackbar("⏱️ 뽀모도로 ${duration}분으로 설정됐어요")
                dialog.dismiss()
            }
            .setNeutralButton("개인정보처리방침") { _, _ ->
                startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
