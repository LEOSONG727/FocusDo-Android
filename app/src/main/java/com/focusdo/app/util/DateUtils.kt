package com.focusdo.app.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")

    fun today(): String = fmt.format(Date())

    fun format(date: Date): String = fmt.format(date)

    fun parse(s: String): Date = runCatching { fmt.parse(s) }.getOrNull() ?: Date()

    fun addDays(s: String, delta: Int): String {
        val cal = Calendar.getInstance()
        cal.time = parse(s)
        cal.add(Calendar.DATE, delta)
        return format(cal.time)
    }

    fun dayOfWeek(s: String): String {
        val cal = Calendar.getInstance()
        cal.time = parse(s)
        return dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun dayOfMonth(s: String): Int {
        val cal = Calendar.getInstance()
        cal.time = parse(s)
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun weekStart(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -7)
        return format(cal.time)
    }

    fun monthStart(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-01".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun formatFocusTime(seconds: Int): String {
        if (seconds < 60) return if (seconds > 0) "${seconds}초" else "0분"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}시간 ${m}분" else "${m}분"
    }

    fun formatTimer(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }
}
