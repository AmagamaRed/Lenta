package com.example.lenta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lenta.ui.calendar.CalendarScreen
import com.example.lenta.ui.easymode.EasyModeScreen
import com.example.lenta.ui.theme.LentaTheme
import com.example.lenta.ui.timeline.TimelineScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.lenta.ui.settings.SettingsScreen
import com.example.lenta.ui.timeline.ViewMode
import com.example.lenta.ui.timeline.TimelineDays
import com.example.lenta.ui.calendar.CalendarViewMode
import com.example.lenta.ui.task.TaskDetailScreen
import com.example.lenta.model.Task
import java.time.LocalDate
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            LentaTheme(darkTheme = isDarkTheme) {
                val viewModel: MainViewModel = viewModel()
                val tasks by viewModel.allTasks.collectAsState()
                val timelineScale by viewModel.timelineScale.collectAsState()
                val timelineStackTasks by viewModel.timelineStackTasks.collectAsState()
                val lockVerticalScroll by viewModel.lockVerticalScroll.collectAsState()
                val timelineMinX by viewModel.timelineMinX.collectAsState()
                val timelineMaxX by viewModel.timelineMaxX.collectAsState()
                val calendarPreference by viewModel.calendarPreference.collectAsState()
                val dayBarColor by viewModel.dayBarColor.collectAsState()
                val dayBarOpacity by viewModel.dayBarOpacity.collectAsState()
                val gridColor by viewModel.gridColor.collectAsState()
                val gridOpacity by viewModel.gridOpacity.collectAsState()
                val taskListColor by viewModel.taskListColor.collectAsState()
                val taskListBrightness by viewModel.taskListBrightness.collectAsState()
                val todayColor by viewModel.todayColor.collectAsState()
                val timelineDaysName by viewModel.timelineDays.collectAsState()
                val timelineDays = try { TimelineDays.valueOf(timelineDaysName) } catch(e: Exception) { TimelineDays.DAY_3 }
                
                var selectedTab by remember { mutableStateOf(0) }
                var timelineViewMode by remember { mutableStateOf(ViewMode.TIMELINE) }
                var calendarViewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
                var showSettings by remember { mutableStateOf(false) }
                var activeSettingsSubScreen by remember { mutableStateOf<String?>(null) }
                
                var showTaskDetail by remember { mutableStateOf(false) }
                var taskToEdit by remember { mutableStateOf<Task?>(null) }
                var initialDateForTask by remember { mutableStateOf<LocalDate?>(null) }

                if (showSettings) {
                    BackHandler {
                        if (activeSettingsSubScreen != null) {
                            activeSettingsSubScreen = null
                        } else {
                            showSettings = false
                        }
                    }
                    SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { isDarkTheme = it },
                        timelineScale = timelineScale,
                        onTimelineScaleChange = { viewModel.setTimelineScale(it) },
                        timelineStackTasks = timelineStackTasks,
                        onTimelineStackTasksChange = { viewModel.setTimelineStackTasks(it) },
                        lockVerticalScroll = lockVerticalScroll,
                        onLockVerticalScrollChange = { viewModel.setLockVerticalScroll(it) },
                        timelineMinX = timelineMinX,
                        onTimelineMinXChange = { viewModel.setTimelineMinX(it) },
                        timelineMaxX = timelineMaxX,
                        onTimelineMaxXChange = { viewModel.setTimelineMaxX(it) },
                        calendarPreference = calendarPreference,
                        onCalendarPreferenceChange = { viewModel.setCalendarPreference(it) },
                        dayBarColor = dayBarColor,
                        onDayBarColorChange = { viewModel.setDayBarColor(it) },
                        dayBarOpacity = dayBarOpacity,
                        onDayBarOpacityChange = { viewModel.setDayBarOpacity(it) },
                        gridColor = gridColor,
                        onGridColorChange = { viewModel.setGridColor(it) },
                        gridOpacity = gridOpacity,
                        onGridOpacityChange = { viewModel.setGridOpacity(it) },
                        taskListColor = taskListColor,
                        onTaskListColorChange = { viewModel.setTaskListColor(it) },
                        taskListBrightness = taskListBrightness,
                        onTaskListBrightnessChange = { viewModel.setTaskListBrightness(it) },
                        todayColor = todayColor,
                        onTodayColorChange = { viewModel.setTodayColor(it) },
                        activeSubScreen = activeSettingsSubScreen,
                        onSubScreenChange = { activeSettingsSubScreen = it },
                        onBack = { 
                            if (activeSettingsSubScreen != null) {
                                activeSettingsSubScreen = null
                            } else {
                                showSettings = false
                            }
                        }
                    )
                } else if (showTaskDetail) {
                    BackHandler {
                        showTaskDetail = false
                        taskToEdit = null
                        initialDateForTask = null
                    }
                    TaskDetailScreen(
                        task = taskToEdit,
                        initialDate = initialDateForTask,
                        onDismiss = { 
                            showTaskDetail = false
                            taskToEdit = null
                            initialDateForTask = null
                        },
                        onSave = { task ->
                            if (task.id != 0L) {
                                viewModel.updateTask(task)
                            } else {
                                viewModel.addTask(task)
                            }
                            showTaskDetail = false
                            taskToEdit = null
                            initialDateForTask = null
                        },
                        onDelete = { task ->
                            viewModel.deleteTask(task)
                            showTaskDetail = false
                            taskToEdit = null
                            initialDateForTask = null
                        }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { 
                                        if (selectedTab == 0) {
                                            timelineViewMode = if (timelineViewMode == ViewMode.TIMELINE) ViewMode.LIST else ViewMode.TIMELINE
                                        } else {
                                            selectedTab = 0 
                                        }
                                    },
                                    icon = { Text("📏") }, // Ruler icon for Timeline
                                    label = { Text("Timeline") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { 
                                        if (selectedTab == 1) {
                                            if (calendarPreference == 0) {
                                                calendarViewMode = if (calendarViewMode == CalendarViewMode.MONTHLY) CalendarViewMode.VERTICAL_LIST else CalendarViewMode.MONTHLY
                                            }
                                        } else {
                                            if (calendarPreference == 1) {
                                                calendarViewMode = CalendarViewMode.VERTICAL_LIST
                                            } else if (calendarPreference == 2) {
                                                calendarViewMode = CalendarViewMode.MONTHLY
                                            }
                                            selectedTab = 1 
                                        }
                                    },
                                    icon = { Text("🗓️") },
                                    label = { Text("Calendar") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Text("💬") },
                                    label = { Text("Easy") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            when (selectedTab) {
                                0 -> TimelineScreen(
                                    tasks = tasks,
                                    onTaskClick = { 
                                        taskToEdit = it
                                        showTaskDetail = true
                                    },
                                    onAddTaskClick = {
                                        taskToEdit = null
                                        showTaskDetail = true
                                    },
                                    viewMode = timelineViewMode,
                                    onViewModeChange = { timelineViewMode = it },
                                    laneScale = timelineScale,
                                    stickTimelines = timelineStackTasks,
                                    lockVerticalScroll = lockVerticalScroll,
                                    customMinX = timelineMinX,
                                    customMaxX = timelineMaxX,
                                    initialTimelineDays = timelineDays,
                                    onTimelineDaysChange = { viewModel.setTimelineDays(it.name) }
                                )
                                1 -> CalendarScreen(
                                    tasks = tasks.filter { !it.isEasyModeEntry },
                                    onTaskClick = {
                                        taskToEdit = it
                                        showTaskDetail = true
                                    },
                                    onAddTaskClick = { date ->
                                        taskToEdit = null
                                        initialDateForTask = date
                                        showTaskDetail = true
                                    },
                                    viewMode = calendarViewMode,
                                    onViewModeChange = { calendarViewMode = it },
                                    onSettingsClick = { showSettings = true },
                                    dayBarColor = dayBarColor,
                                    dayBarOpacity = dayBarOpacity,
                                    gridColor = gridColor,
                                    gridOpacity = gridOpacity,
                                    taskListColor = taskListColor,
                                    taskListBrightness = taskListBrightness,
                                    todayColor = todayColor
                                )
                                2 -> EasyModeScreen(
                                    tasks = tasks,
                                    onAddTask = { viewModel.addTask(it) },
                                    onUpdateTask = { viewModel.updateTask(it) },
                                    onDeleteTask = { viewModel.deleteTask(it) },
                                    onDeleteCompleted = { viewModel.deleteCompletedEasyModeTasks() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
