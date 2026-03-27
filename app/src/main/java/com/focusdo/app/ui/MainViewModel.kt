package com.focusdo.app.ui

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.focusdo.app.data.AppDatabase
import com.focusdo.app.data.FocusState
import com.focusdo.app.data.Task
import com.focusdo.app.service.PomodoroService
import com.focusdo.app.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("focusdo_prefs", Context.MODE_PRIVATE)
    private val dao   = AppDatabase.getInstance(app).taskDao()

    // ── DB streams ───────────────────────────────────────────────────────────

    val allTasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate

    private val _filter = MutableStateFlow("all")

    val visibleTasks: StateFlow<List<Task>> =
        combine(allTasks, _selectedDate, _filter) { tasks, date, f ->
            val daily = tasks.filter { it.date == date }
            if (f == "completed") daily.filter { it.completed } else daily
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievement: StateFlow<Int> =
        combine(allTasks, _selectedDate) { tasks, date ->
            val daily = tasks.filter { it.date == date }
            if (daily.isEmpty()) 0 else (daily.count { it.completed } * 100 / daily.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stats: StateFlow<Stats> = allTasks.map { tasks ->
        val today = DateUtils.today()
        val todayTasks = tasks.filter { it.date == today }
        Stats(
            dayTime     = todayTasks.sumOf { it.focusedTime },
            weekTime    = tasks.filter { it.date >= DateUtils.weekStart() }.sumOf { it.focusedTime },
            monthTime   = tasks.filter { it.date >= DateUtils.monthStart() }.sumOf { it.focusedTime },
            dayTomatoes = todayTasks.sumOf { it.tomatoes }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())

    // ── Settings ─────────────────────────────────────────────────────────────

    private val _pomodoroDuration = MutableStateFlow(prefs.getInt("pomodoro_duration", 25))
    val pomodoroDuration: StateFlow<Int> = _pomodoroDuration

    fun setPomodoroDuration(minutes: Int) {
        _pomodoroDuration.value = minutes
        prefs.edit().putInt("pomodoro_duration", minutes).apply()
    }

    // ── Focus — state lives in PomodoroService ────────────────────────────────
    //
    // Timer runs inside a Foreground Service so it keeps ticking when the
    // user leaves the app. We alias the service's shared flows here so
    // the rest of the UI doesn't need to know where the state comes from.

    val focus: StateFlow<FocusState> = PomodoroService.focusState
    val toast: SharedFlow<String>    = PomodoroService.toast

    private var todaySessionCount = 0

    fun startFocus(taskId: Long) {
        todaySessionCount++
        val taskTitle = allTasks.value.find { it.id == taskId }?.title ?: ""
        val ctx = getApplication<Application>()
        ContextCompat.startForegroundService(
            ctx,
            PomodoroService.startIntent(ctx, taskId, taskTitle, _pomodoroDuration.value, todaySessionCount)
        )
    }

    fun togglePause() {
        val ctx = getApplication<Application>()
        ctx.startService(PomodoroService.pauseIntent(ctx))
    }

    fun stopFocus() {
        val ctx = getApplication<Application>()
        ctx.startService(PomodoroService.stopIntent(ctx))
    }

    // ── Task CRUD ─────────────────────────────────────────────────────────────

    fun selectDate(date: String) { _selectedDate.value = date }
    fun prevDate()  { _selectedDate.value = DateUtils.addDays(_selectedDate.value, -1) }
    fun nextDate()  { _selectedDate.value = DateUtils.addDays(_selectedDate.value,  1) }
    fun setFilter(f: String) { _filter.value = f }

    fun addTask(title: String, date: String): Long {
        val id = System.currentTimeMillis()
        viewModelScope.launch { dao.insert(Task(id = id, title = title, date = date)) }
        return id
    }

    fun updateTask(task: Task) { viewModelScope.launch { dao.update(task) } }
    fun deleteTask(task: Task) { viewModelScope.launch { dao.delete(task) } }
    fun toggleComplete(task: Task) {
        viewModelScope.launch { dao.update(task.copy(completed = !task.completed)) }
    }

    data class Stats(
        val dayTime: Int = 0,
        val weekTime: Int = 0,
        val monthTime: Int = 0,
        val dayTomatoes: Int = 0
    )
}
