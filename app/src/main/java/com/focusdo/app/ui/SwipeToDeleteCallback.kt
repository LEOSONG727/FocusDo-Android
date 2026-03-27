package com.focusdo.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.focusdo.app.R

/**
 * Swipe-left-to-delete with a soft pink/red reveal background and trash icon.
 * Matches the cute/casual app aesthetic — not harsh, just a gentle hint.
 */
abstract class SwipeToDeleteCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEBEE") // very light red — soft, not alarming
    }
    private val bgCornerRadius = context.resources.getDimension(R.dimen.radius_md)
    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.spacing_xl)
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.apply {
        setTint(Color.parseColor("#E53935"))
    }

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val item = viewHolder.itemView
        if (dX >= 0) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Soft rounded background that grows with the swipe
        val bgRect = RectF(
            item.right + dX - bgCornerRadius,
            item.top.toFloat(),
            item.right.toFloat() + bgCornerRadius,
            item.bottom.toFloat()
        )
        // Clip so rounded left corners are hidden behind the card
        c.save()
        c.clipRect(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat())
        c.drawRoundRect(bgRect, bgCornerRadius, bgCornerRadius, bgPaint)
        c.restore()

        // Trash icon — appears once enough is revealed
        val swipeRatio = (-dX) / item.width
        if (swipeRatio > 0.15f) {
            deleteIcon?.let { icon ->
                val iconSize = icon.intrinsicWidth
                val iconTop = item.top + (item.height - iconSize) / 2
                val iconLeft = item.right - iconMargin - iconSize
                icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                icon.alpha = ((swipeRatio - 0.15f) / 0.2f * 255).toInt().coerceIn(0, 255)
                icon.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
