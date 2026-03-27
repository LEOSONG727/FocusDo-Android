package com.focusdo.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusdo.app.R
import com.focusdo.app.data.AppDatabase
import com.focusdo.app.data.FocusState
import com.focusdo.app.ui.MainActivity
import com.focusdo.app.util.DateUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Foreground Service that owns the Pomodoro timer.
 *
 * The timer runs here — not in the ViewModel — so it keeps ticking when
 * the user switches apps or locks the screen. State is exposed through
 * companion-object StateFlows that the ViewModel simply aliases.
 *
 * Flow:
 *   MainActivity / ViewModel  →  startForegroundService(startIntent)
 *   PomodoroService           →  updates focusState / toast every second
 *   ViewModel / UI            →  observes focusState / toast
 */
class PomodoroService : Service() {

    // ── Shared state (observed by MainViewModel) ──────────────────────────────
    companion object {
        val focusState = MutableStateFlow(FocusState())
        val toast      = MutableSharedFlow<String>(extraBufferCapacity = 4)

        // Intent actions
        const val ACTION_START = "com.focusdo.START"
        const val ACTION_PAUSE = "com.focusdo.PAUSE"
        const val ACTION_STOP  = "com.focusdo.STOP"

        // Extras
        const val EXTRA_TASK_ID    = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_DURATION   = "duration_min"
        const val EXTRA_SESSION    = "session_count"

        // Notification IDs
        private const val NOTIF_TIMER    = 1001
        private const val NOTIF_COMPLETE = 1002
        const val CHANNEL_ID = "focusdo_timer"

        // Factory helpers called from ViewModel
        fun startIntent(ctx: Context, taskId: Long, title: String, durationMin: Int, session: Int) =
            Intent(ctx, PomodoroService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_DURATION, durationMin)
                putExtra(EXTRA_SESSION, session)
            }

        fun pauseIntent(ctx: Context) =
            Intent(ctx, PomodoroService::class.java).apply { action = ACTION_PAUSE }

        fun stopIntent(ctx: Context) =
            Intent(ctx, PomodoroService::class.java).apply { action = ACTION_STOP }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val dao by lazy { AppDatabase.getInstance(applicationContext).taskDao() }
    private var timerJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId  = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val title   = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""
                val dur     = intent.getIntExtra(EXTRA_DURATION, 25)
                val session = intent.getIntExtra(EXTRA_SESSION, 1)
                startTimer(taskId, title, dur, session)
            }
            ACTION_PAUSE -> togglePause()
            ACTION_STOP  -> stopTimer()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer(taskId: Long, title: String, durationMin: Int, session: Int) {
        timerJob?.cancel()
        focusState.value = FocusState(active = true, taskId = taskId, sessionCount = session)

        // Show foreground notification immediately
        startForeground(NOTIF_TIMER, buildTimerNotification(title, durationMin * 60, paused = false))

        timerJob = scope.launch {
            val pomSec = durationMin * 60
            while (isActive) {
                delay(1000L)
                val s = focusState.value
                if (!s.active) break

                if (s.paused) {
                    // Refresh notification so pause button text stays correct
                    refreshNotification(title, pomSec - s.cycleElapsed, paused = true)
                    continue
                }

                val newCycle = s.cycleElapsed + 1

                if (newCycle >= pomSec) {
                    // 🍅 One cycle complete
                    onCycleComplete(s, title, durationMin)
                    refreshNotification(title, pomSec, paused = false)
                } else {
                    focusState.value = s.copy(
                        totalElapsed = s.totalElapsed + 1,
                        cycleElapsed = newCycle
                    )
                    refreshNotification(title, pomSec - newCycle, paused = false)
                }
            }
        }
    }

    private fun togglePause() {
        val s = focusState.value
        focusState.value = s.copy(paused = !s.paused)
    }

    private fun stopTimer() {
        timerJob?.cancel()
        val s = focusState.value
        if (!s.active) { stopSelf(); return }

        scope.launch {
            // Persist accumulated focus time to database
            val task = dao.getById(s.taskId)
            if (task != null) {
                dao.update(task.copy(focusedTime = task.focusedTime + s.totalElapsed))
                val min = s.totalElapsed / 60
                val msg = when {
                    s.sessionTomatoes > 0 ->
                        "토마토 ${s.sessionTomatoes}개, ${min}분 집중 완료! 대단해요! 🏆"
                    s.totalElapsed >= 60  -> "${min}분 집중 완료! 수고하셨어요! 👏"
                    s.totalElapsed >= 30  -> "${s.totalElapsed}초 집중 완료! 수고하셨어요! 👏"
                    else                  -> "다음에는 조금 더 집중해봐요! 💪"
                }
                toast.emit(msg)
            }
            focusState.value = FocusState()   // reset shared state
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun onCycleComplete(s: FocusState, title: String, durationMin: Int) {
        val task = dao.getById(s.taskId)
        if (task != null) {
            dao.update(task.copy(tomatoes = task.tomatoes + 1))
            toast.emit("🍅 뽀모 완료! \"${task.title}\" 에 기록했어요 ✨")
        }
        val newTomatoes = s.sessionTomatoes + 1
        focusState.value = s.copy(
            totalElapsed    = s.totalElapsed + 1,
            cycleElapsed    = 0,
            sessionTomatoes = newTomatoes
        )
        // Fire a heads-up completion notification
        sendCompletionNotification(title, newTomatoes)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun refreshNotification(title: String, remainingSec: Int, paused: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_TIMER, buildTimerNotification(title, remainingSec, paused))
    }

    private fun buildTimerNotification(title: String, remainingSec: Int, paused: Boolean): Notification {
        val timeStr    = DateUtils.formatTimer(remainingSec)
        val statusLine = if (paused) "⏸ 일시정지 — $timeStr 남음" else "⏱ $timeStr 남음"
        val pauseLabel = if (paused) "다시 시작" else "일시 정지"

        val tapPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pausePi = PendingIntent.getService(
            this, 1,
            pauseIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPi = PendingIntent.getService(
            this, 2,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(title.ifEmpty { "집중 중" })
            .setContentText(statusLine)
            .setContentIntent(tapPi)
            .addAction(0, pauseLabel, pausePi)
            .addAction(0, "종료", stopPi)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun sendCompletionNotification(taskTitle: String, tomatoCount: Int) {
        val tapPi = PendingIntent.getActivity(
            this, 3,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("🍅 뽀모도로 완료!")
            .setContentText("\"$taskTitle\" — 오늘 ${tomatoCount}번째 집중 완료 🎉")
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_COMPLETE, n)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "집중 타이머",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "뽀모도로 타이머 진행 상황을 표시합니다"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
