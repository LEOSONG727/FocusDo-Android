package com.focusdo.app.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * Circular progress ring for the Pomodoro timer.
 * progress = 1.0 → full ring (just started), 0.0 → empty ring (finished)
 */
class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val strokeWidth = context.resources.displayMetrics.density * 10f

    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@TimerRingView.strokeWidth
        color = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY
        )
    }

    private val paintArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@TimerRingView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimary, Color.BLUE
        )
    }

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@TimerRingView.strokeWidth + 8f
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        color = MaterialColors.getColor(
            context, com.google.android.material.R.attr.colorPrimary, Color.BLUE
        ).let { Color.argb(60, Color.red(it), Color.green(it), Color.blue(it)) }
    }

    private val oval = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val inset = strokeWidth / 2f + 6f
        oval.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        // Background ring
        canvas.drawOval(oval, paintBg)

        // Progress arc (12 o'clock = -90°, clockwise)
        val sweep = 360f * progress
        if (sweep > 0f) {
            canvas.drawArc(oval, -90f, sweep, false, paintGlow)
            canvas.drawArc(oval, -90f, sweep, false, paintArc)
        }
    }
}
