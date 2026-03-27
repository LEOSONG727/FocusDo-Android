package com.focusdo.app.data

/**
 * Shared state for the Pomodoro timer.
 * Lives in the data package so both PomodoroService and MainViewModel
 * can reference it without circular imports.
 */
data class FocusState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val taskId: Long = -1L,
    val totalElapsed: Int = 0,
    val cycleElapsed: Int = 0,
    val sessionTomatoes: Int = 0,
    val sessionCount: Int = 0
)
