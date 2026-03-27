package com.focusdo.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.focusdo.app.databinding.ItemCalDayBinding
import com.focusdo.app.util.DateUtils

class CalendarAdapter(
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    private var dates: List<String> = emptyList()
    private var selectedDate: String = DateUtils.today()

    fun submitDates(newDates: List<String>, selected: String) {
        dates = newDates
        selectedDate = selected
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemCalDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCalDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val date = dates[position]
        val isActive = date == selectedDate
        val isToday = date == DateUtils.today()
        with(holder.binding) {
            tvWeekday.text = DateUtils.dayOfWeek(date)
            tvDay.text = DateUtils.dayOfMonth(date).toString()
            root.isSelected = isActive
            vTodayDot.visibility = if (isToday) android.view.View.VISIBLE else android.view.View.GONE
            root.setOnClickListener { onDateClick(date) }
        }
    }

    override fun getItemCount() = dates.size
}
