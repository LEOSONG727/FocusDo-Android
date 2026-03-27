package com.focusdo.app.ui

import android.graphics.Paint
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusdo.app.data.Task
import com.focusdo.app.databinding.ItemTaskBinding
import com.focusdo.app.util.DateUtils

class TaskAdapter(
    private val onToggle: (Task) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onStartFocus: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(a: Task, b: Task) = a.id == b.id
            override fun areContentsTheSame(a: Task, b: Task) = a == b
        }
    }

    inner class VH(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        with(holder.binding) {
            tvTitle.text = task.title
            tvMeta.text = "⚡ ${DateUtils.formatFocusTime(task.focusedTime)}  🍅 ${task.tomatoes}"

            val done = task.completed
            cbTask.isChecked = done
            tvTitle.paintFlags = if (done)
                tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            root.alpha = if (done) 0.52f else 1f
            vAccent.visibility = if (done) View.GONE else View.VISIBLE

            cbTask.setOnClickListener {
                // Haptic
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                // Satisfying bounce: pop up then spring back
                it.animate()
                    .scaleX(1.35f).scaleY(1.35f)
                    .setDuration(90)
                    .withEndAction {
                        it.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
                onToggle(task)
            }
            btnFocus.setOnClickListener { onStartFocus(task) }
            root.setOnClickListener { onEdit(task) }
        }
    }

    fun getTaskAt(position: Int): Task = getItem(position)
}
