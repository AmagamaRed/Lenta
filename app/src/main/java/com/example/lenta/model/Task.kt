package com.example.lenta.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val importance: Importance = Importance.MEDIUM,
    val recurrence: Recurrence = Recurrence.NONE,
    val categoryId: Long? = null,
    val color: Int? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isEasyModeEntry: Boolean = false,
    val isNumbered: Boolean = false,
    val hasCheckbox: Boolean = false
)