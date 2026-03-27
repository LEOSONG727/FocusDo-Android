package com.focusdo.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Canvas-drawn Chiikawa-style study buddy character.
 * Bobs gently up and down while focus is active; freezes when paused.
 */
class BuddyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun Float.dp() = this * density
    private fun Int.dp() = this.toFloat() * density

    private val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2D2D2D")
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.5f.dp()
    }
    private val paintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2D2D2D")
    }
    private val paintBlush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFB3C6")
        alpha = 180
    }
    private val paintTool = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#DDEEFF")
    }
    private val paintTable = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FDEBD0")
    }
    private val mouthPath = Path()

    private var bobY = 0f
    private val bobAnimator = ValueAnimator.ofFloat(-4f.dp(), 4f.dp()).apply {
        duration = 1800L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            bobY = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bobAnimator.start()
    }

    override fun onDetachedFromWindow() {
        bobAnimator.cancel()
        super.onDetachedFromWindow()
    }

    fun setPaused(paused: Boolean) {
        alpha = if (paused) 0.6f else 1.0f
        if (paused) bobAnimator.pause() else bobAnimator.resume()
    }

    fun setOnDark(onDark: Boolean) {
        paintBorder.color = if (onDark) Color.parseColor("#BBBBCC") else Color.parseColor("#2D2D2D")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f

        // ── Measurements ────────────────────────────────────────────────────
        val tableY    = height * 0.90f
        val bodyH     = 32.dp()
        val bodyW     = 66.dp()
        val bodyTop   = tableY - bodyH + bobY
        val bodyBtm   = tableY + bobY
        val headH     = 52.dp()
        val headW     = 62.dp()
        val headBtm   = bodyTop + 8.dp()
        val headTop   = headBtm - headH
        val earR      = 11.dp()

        // ── Table ────────────────────────────────────────────────────────────
        val tblRect = RectF(cx - 58.dp(), tableY, cx + 58.dp(), tableY + 5.dp())
        canvas.drawRoundRect(tblRect, 3.dp(), 3.dp(), paintTable)
        paintBorder.strokeWidth = 1.8f.dp()
        canvas.drawRoundRect(tblRect, 3.dp(), 3.dp(), paintBorder)
        paintBorder.strokeWidth = 2.5f.dp()

        // ── Body ─────────────────────────────────────────────────────────────
        val bodyRect = RectF(cx - bodyW / 2, bodyTop, cx + bodyW / 2, bodyBtm)
        canvas.drawRoundRect(bodyRect, 18.dp(), 18.dp(), paintWhite)
        canvas.drawRoundRect(bodyRect, 18.dp(), 18.dp(), paintBorder)

        // ── Tool (laptop) on body ─────────────────────────────────────────
        val toolW = 26.dp(); val toolH = 10.dp()
        val toolRect = RectF(cx - toolW / 2, bodyBtm - toolH - 4.dp(), cx + toolW / 2, bodyBtm - 4.dp())
        canvas.drawRoundRect(toolRect, 2.dp(), 2.dp(), paintTool)
        paintBorder.strokeWidth = 1.5f.dp()
        canvas.drawRoundRect(toolRect, 2.dp(), 2.dp(), paintBorder)
        paintBorder.strokeWidth = 2.5f.dp()

        // ── Hands ────────────────────────────────────────────────────────────
        val handR = 7.dp()
        val handY = bodyTop + 11.dp()
        canvas.drawCircle(cx - bodyW / 2 - handR * 0.5f, handY, handR, paintWhite)
        canvas.drawCircle(cx - bodyW / 2 - handR * 0.5f, handY, handR, paintBorder)
        canvas.drawCircle(cx + bodyW / 2 + handR * 0.5f, handY, handR, paintWhite)
        canvas.drawCircle(cx + bodyW / 2 + handR * 0.5f, handY, handR, paintBorder)

        // ── Ears (behind head) ───────────────────────────────────────────────
        canvas.drawCircle(cx - headW / 2 + earR, headTop + 5.dp(), earR, paintWhite)
        canvas.drawCircle(cx - headW / 2 + earR, headTop + 5.dp(), earR, paintBorder)
        canvas.drawCircle(cx + headW / 2 - earR, headTop + 5.dp(), earR, paintWhite)
        canvas.drawCircle(cx + headW / 2 - earR, headTop + 5.dp(), earR, paintBorder)

        // ── Head ─────────────────────────────────────────────────────────────
        val headRect = RectF(cx - headW / 2, headTop + 5.dp(), cx + headW / 2, headBtm)
        canvas.drawRoundRect(headRect, 26.dp(), 26.dp(), paintWhite)
        canvas.drawRoundRect(headRect, 26.dp(), 26.dp(), paintBorder)

        // ── Face elements ────────────────────────────────────────────────────
        val eyeY   = headTop + headH * 0.44f
        val eyeW   = 4.dp();  val eyeH = 5.dp()

        // Eyes
        canvas.drawOval(RectF(cx - 14.dp(), eyeY - eyeH / 2, cx - 14.dp() + eyeW, eyeY + eyeH / 2), paintDark)
        canvas.drawOval(RectF(cx + 10.dp(), eyeY - eyeH / 2, cx + 10.dp() + eyeW, eyeY + eyeH / 2), paintDark)

        // Nose
        val noseY = eyeY + 7.dp()
        canvas.drawRoundRect(RectF(cx - 2.dp(), noseY, cx + 2.dp(), noseY + 3.dp()), 2.dp(), 2.dp(), paintDark)

        // Mouth
        val mouthY = noseY + 5.5f.dp()
        mouthPath.reset()
        mouthPath.moveTo(cx - 5.dp(), mouthY)
        mouthPath.quadTo(cx, mouthY + 5.dp(), cx + 5.dp(), mouthY)
        paintBorder.strokeWidth = 1.8f.dp()
        canvas.drawPath(mouthPath, paintBorder)
        paintBorder.strokeWidth = 2.5f.dp()

        // Blush
        canvas.drawOval(RectF(cx - 26.dp(), eyeY + 3.dp(), cx - 16.dp(), eyeY + 8.dp()), paintBlush)
        canvas.drawOval(RectF(cx + 16.dp(), eyeY + 3.dp(), cx + 26.dp(), eyeY + 8.dp()), paintBlush)
    }
}
