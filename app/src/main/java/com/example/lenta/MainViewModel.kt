package com.example.lenta

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lenta.data.AppDatabase
import com.example.lenta.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val prefs = application.getSharedPreferences("lenta_settings", Context.MODE_PRIVATE)

    private val _timelineScale = MutableStateFlow(prefs.getFloat("timeline_scale", 1f))
    val timelineScale = _timelineScale.asStateFlow()

    fun setTimelineScale(scale: Float) {
        _timelineScale.value = scale
        prefs.edit().putFloat("timeline_scale", scale).apply()
    }

    private val _timelineStackTasks = MutableStateFlow(prefs.getBoolean("timeline_stack", false))
    val timelineStackTasks = _timelineStackTasks.asStateFlow()

    fun setTimelineStackTasks(stack: Boolean) {
        _timelineStackTasks.value = stack
        prefs.edit().putBoolean("timeline_stack", stack).apply()
    }

    private val _lockVerticalScroll = MutableStateFlow(prefs.getBoolean("lock_scroll", false))
    val lockVerticalScroll = _lockVerticalScroll.asStateFlow()

    fun setLockVerticalScroll(lock: Boolean) {
        _lockVerticalScroll.value = lock
        prefs.edit().putBoolean("lock_scroll", lock).apply()
    }

    private val _timelineMinX = MutableStateFlow(prefs.getFloat("timeline_min_x", -2500f))
    val timelineMinX = _timelineMinX.asStateFlow()

    fun setTimelineMinX(value: Float) {
        _timelineMinX.value = value
        prefs.edit().putFloat("timeline_min_x", value).apply()
    }

    private val _timelineMaxX = MutableStateFlow(prefs.getFloat("timeline_max_x", 3400f))
    val timelineMaxX = _timelineMaxX.asStateFlow()

    fun setTimelineMaxX(value: Float) {
        _timelineMaxX.value = value
        prefs.edit().putFloat("timeline_max_x", value).apply()
    }

    private val _timelineDays = MutableStateFlow(prefs.getString("timeline_days", "DAY_3") ?: "DAY_3")
    val timelineDays = _timelineDays.asStateFlow()

    fun setTimelineDays(days: String) {
        _timelineDays.value = days
        prefs.edit().putString("timeline_days", days).apply()
    }

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insert(task)
        }
    }

    fun addTask(title: String) {
        addTask(
            Task(
                title = title,
                isEasyModeEntry = true,
                hasCheckbox = true
            )
        )
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun deleteCompletedEasyModeTasks() {
        viewModelScope.launch {
            val tasksToDelete = allTasks.value.filter { it.isEasyModeEntry && it.isCompleted }
            tasksToDelete.forEach { taskDao.delete(it) }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.delete(task)
        }
    }
}