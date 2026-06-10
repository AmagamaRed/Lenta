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

    private val _calendarPreference = MutableStateFlow(prefs.getInt("calendar_pref", 0)) // 0: Both, 1: V, 2: G
    val calendarPreference = _calendarPreference.asStateFlow()

    fun setCalendarPreference(pref: Int) {
        _calendarPreference.value = pref
        prefs.edit().putInt("calendar_pref", pref).apply()
    }

    private val _dayBarColor = MutableStateFlow(prefs.getInt("day_bar_color", -1)) // Default to -1 or some sensible color
    val dayBarColor = _dayBarColor.asStateFlow()
    fun setDayBarColor(color: Int) {
        _dayBarColor.value = color
        prefs.edit().putInt("day_bar_color", color).apply()
    }

    private val _dayBarOpacity = MutableStateFlow(prefs.getFloat("day_bar_opacity", 1.0f))
    val dayBarOpacity = _dayBarOpacity.asStateFlow()
    fun setDayBarOpacity(opacity: Float) {
        _dayBarOpacity.value = opacity
        prefs.edit().putFloat("day_bar_opacity", opacity).apply()
    }

    private val _gridColor = MutableStateFlow(prefs.getInt("grid_color", -3355444)) // Default gray
    val gridColor = _gridColor.asStateFlow()
    fun setGridColor(color: Int) {
        _gridColor.value = color
        prefs.edit().putInt("grid_color", color).apply()
    }

    private val _gridOpacity = MutableStateFlow(prefs.getFloat("grid_opacity", 0.5f))
    val gridOpacity = _gridOpacity.asStateFlow()
    fun setGridOpacity(opacity: Float) {
        _gridOpacity.value = opacity
        prefs.edit().putFloat("grid_opacity", opacity).apply()
    }

    private val _taskListColor = MutableStateFlow(prefs.getInt("task_list_color", -1)) // Default -1 (surface)
    val taskListColor = _taskListColor.asStateFlow()
    fun setTaskListColor(color: Int) {
        _taskListColor.value = color
        prefs.edit().putInt("task_list_color", color).apply()
    }

    private val _taskListOpacity = MutableStateFlow(prefs.getFloat("task_list_opacity", 1.0f))
    val taskListOpacity = _taskListOpacity.asStateFlow()
    fun setTaskListOpacity(opacity: Float) {
        _taskListOpacity.value = opacity
        prefs.edit().putFloat("task_list_opacity", opacity).apply()
    }

    private val _taskListBrightness = MutableStateFlow(prefs.getFloat("task_list_brightness", 1.0f))
    val taskListBrightness = _taskListBrightness.asStateFlow()
    fun setTaskListBrightness(value: Float) {
        _taskListBrightness.value = value
        prefs.edit().putFloat("task_list_brightness", value).apply()
    }

    private val _todayColor = MutableStateFlow(prefs.getInt("today_color", -16738680)) // Default Material blue
    val todayColor = _todayColor.asStateFlow()
    fun setTodayColor(color: Int) {
        _todayColor.value = color
        prefs.edit().putInt("today_color", color).apply()
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