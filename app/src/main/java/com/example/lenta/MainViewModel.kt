package com.example.lenta

import android.app.Application
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

    private val _timelineScale = MutableStateFlow(1f)
    val timelineScale = _timelineScale.asStateFlow()

    fun setTimelineScale(scale: Float) {
        _timelineScale.value = scale
    }

    private val _timelineStackTasks = MutableStateFlow(false)
    val timelineStackTasks = _timelineStackTasks.asStateFlow()

    fun setTimelineStackTasks(stack: Boolean) {
        _timelineStackTasks.value = stack
    }

    private val _timelineDays = MutableStateFlow("DAY_1")
    val timelineDays = _timelineDays.asStateFlow()

    fun setTimelineDays(days: String) {
        _timelineDays.value = days
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