package com.example.lenta

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lenta.data.AppDatabase
import com.example.lenta.model.Category
import com.example.lenta.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()
    private val prefs = application.getSharedPreferences("lenta_settings", Context.MODE_PRIVATE)

    private val _timelineScale = MutableStateFlow(prefs.getFloat("timeline_scale", 1f))
    val timelineScale = _timelineScale.asStateFlow()

    fun setTimelineScale(scale: Float) {
        _timelineScale.value = scale
        prefs.edit().putFloat("timeline_scale", scale).apply()
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

    private val _warnBeforeDelete = MutableStateFlow(prefs.getBoolean("warn_delete", false))
    val warnBeforeDelete = _warnBeforeDelete.asStateFlow()

    fun setWarnBeforeDelete(warn: Boolean) {
        _warnBeforeDelete.value = warn
        prefs.edit().putBoolean("warn_delete", warn).apply()
    }

    private val _easyModeChatLayout = MutableStateFlow(prefs.getBoolean("easy_mode_chat_layout", false))
    val easyModeChatLayout = _easyModeChatLayout.asStateFlow()

    fun setEasyModeChatLayout(enabled: Boolean) {
        _easyModeChatLayout.value = enabled
        prefs.edit().putBoolean("easy_mode_chat_layout", enabled).apply()
    }

    private val _activeChatId = MutableStateFlow(prefs.getLong("active_chat_id", -1L)) // -1 for "Main/Default"
    val activeChatId = _activeChatId.asStateFlow()

    fun setActiveChatId(id: Long) {
        _activeChatId.value = id
        prefs.edit().putLong("active_chat_id", id).apply()
    }

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _disableTravelTime = MutableStateFlow(prefs.getBoolean("disable_travel_time", false))
    val disableTravelTime = _disableTravelTime.asStateFlow()

    fun setDisableTravelTime(disable: Boolean) {
        _disableTravelTime.value = disable
        prefs.edit().putBoolean("disable_travel_time", disable).apply()
    }

    private val _hideLocation = MutableStateFlow(prefs.getBoolean("hide_location", false))
    val hideLocation = _hideLocation.asStateFlow()
    fun setHideLocation(hide: Boolean) {
        _hideLocation.value = hide
        prefs.edit().putBoolean("hide_location", hide).apply()
    }

    private val _hideUrl = MutableStateFlow(prefs.getBoolean("hide_url", false))
    val hideUrl = _hideUrl.asStateFlow()
    fun setHideUrl(hide: Boolean) {
        _hideUrl.value = hide
        prefs.edit().putBoolean("hide_url", hide).apply()
    }

    private val _hideDescription = MutableStateFlow(prefs.getBoolean("hide_description", false))
    val hideDescription = _hideDescription.asStateFlow()
    fun setHideDescription(hide: Boolean) {
        _hideDescription.value = hide
        prefs.edit().putBoolean("hide_description", hide).apply()
    }

    private val _hideMultiDay = MutableStateFlow(prefs.getBoolean("hide_multi_day", false))
    val hideMultiDay = _hideMultiDay.asStateFlow()
    fun setHideMultiDay(hide: Boolean) {
        _hideMultiDay.value = hide
        prefs.edit().putBoolean("hide_multi_day", hide).apply()
    }

    private val _hideRecurrence = MutableStateFlow(prefs.getBoolean("hide_recurrence", false))
    val hideRecurrence = _hideRecurrence.asStateFlow()
    fun setHideRecurrence(hide: Boolean) {
        _hideRecurrence.value = hide
        prefs.edit().putBoolean("hide_recurrence", hide).apply()
    }

    private val _disableTimelineListToggle = MutableStateFlow(prefs.getBoolean("disable_timeline_toggle", false))
    val disableTimelineListToggle = _disableTimelineListToggle.asStateFlow()
    fun setDisableTimelineListToggle(disabled: Boolean) {
        _disableTimelineListToggle.value = disabled
        prefs.edit().putBoolean("disable_timeline_toggle", disabled).apply()
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

    fun addTask(title: String, hasCheckbox: Boolean = false) {
        addTask(
            Task(
                title = title,
                isEasyModeEntry = true,
                hasCheckbox = hasCheckbox,
                categoryId = if (activeChatId.value == -1L) null else activeChatId.value
            )
        )
    }

    fun addChat(name: String, color: Int) {
        viewModelScope.launch {
            categoryDao.insert(Category(name = name, color = color))
        }
    }

    fun deleteChat(category: Category) {
        viewModelScope.launch {
            categoryDao.delete(category)
            if (activeChatId.value == category.id) {
                setActiveChatId(-1L)
            }
        }
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

    fun deleteTasksInRecurrence(task: Task, deleteAllFollowing: Boolean) {
        viewModelScope.launch {
            if (task.recurrenceId == null) {
                taskDao.delete(task)
            } else {
                if (deleteAllFollowing) {
                    val tasksToDelete = allTasks.value.filter { 
                        it.recurrenceId == task.recurrenceId && (it.startTime ?: 0L) >= (task.startTime ?: 0L)
                    }
                    tasksToDelete.forEach { taskDao.delete(it) }
                } else {
                    taskDao.delete(task)
                }
            }
        }
    }
}