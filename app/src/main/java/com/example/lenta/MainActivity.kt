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
import com.example.lenta.ui.timeline.ViewMode
import com.example.lenta.ui.calendar.CalendarViewMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LentaTheme {
                val viewModel: MainViewModel = viewModel()
                val tasks by viewModel.allTasks.collectAsState()
                var selectedTab by remember { mutableStateOf(0) }
                var timelineViewMode by remember { mutableStateOf(ViewMode.TIMELINE) }
                var calendarViewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }

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
                                icon = { Text("〰️") }, // Streamline/Timeline icon
                                label = { Text("Timeline") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { 
                                    if (selectedTab == 1) {
                                        calendarViewMode = if (calendarViewMode == CalendarViewMode.MONTHLY) CalendarViewMode.VERTICAL_LIST else CalendarViewMode.MONTHLY
                                    } else {
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
                                onAddTask = { viewModel.addTask(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                viewMode = timelineViewMode,
                                onViewModeChange = { timelineViewMode = it }
                            )
                            1 -> CalendarScreen(
                                tasks = tasks.filter { !it.isEasyModeEntry },
                                onAddTask = { viewModel.addTask(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                viewMode = calendarViewMode,
                                onViewModeChange = { calendarViewMode = it }
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
