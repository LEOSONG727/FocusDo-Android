package com.focusdo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val title: String,
    val date: String,          // "YYYY-MM-DD"
    val completed: Boolean = false,
    val focusedTime: Int = 0,  // seconds
    val tomatoes: Int = 0
)
