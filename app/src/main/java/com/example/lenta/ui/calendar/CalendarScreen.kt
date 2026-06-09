package com.example.lenta.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenta.model.Task
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

enum class CalendarViewMode { MONTHLY, VERTICAL_LIST }

@Composable
fun CalendarScreen(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onAddTaskClick: (LocalDate) -> Unit,
    viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    onViewModeChange: (CalendarViewMode) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val tasksOnSelectedDate = remember(tasks, selectedDate) {
        selectedDate?.let { date ->
            tasks.filter { task ->
                task.startTime?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == date
                } ?: false
            }
        } ?: emptyList()
    }

    if (selectedDate != null) {
        BackHandler {
            selectedDate = null
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineMedium
            )
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { 
                    coroutineScope.launch {
                        state.scrollToMonth(YearMonth.now())
                        selectedDate = LocalDate.now()
                    }
                }) {
                    Text("🎯") // Today icon
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            // Unify calendar weight to give 45% of the screen to the task list in both modes when opened
            val mainContentWeight = if (selectedDate != null) 0.55f else 1.0f
            
            Box(modifier = Modifier.weight(mainContentWeight)) {
                if (viewMode == CalendarViewMode.MONTHLY) {
                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            val dayTasks = tasks.filter { task ->
                                task.startTime?.let {
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == day.date
                                } ?: false
                            }
                            Day(
                                day = day,
                                isSelected = selectedDate == day.date,
                                dayTasks = dayTasks,
                                onClick = { selectedDate = it.date }
                            )
                        },
                        monthHeader = { month ->
                            val daysOfWeek = month.weekDays.first().map { it.date.dayOfWeek }
                            MonthHeader(month = month, daysOfWeek = daysOfWeek)
                        }
                    )
                } else {
                    VerticalCalendar(
                        state = state,
                        dayContent = { day ->
                            if (day.position == DayPosition.MonthDate) {
                                val dayTasks = tasks.filter { task ->
                                    task.startTime?.let {
                                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == day.date
                                    } ?: false
                                }
                                VerticalDayCell(
                                    day = day,
                                    dayTasks = dayTasks,
                                    isSelected = selectedDate == day.date,
                                    onClick = { selectedDate = it.date }
                                )
                            }
                        },
                        monthHeader = { month ->
                            Text(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                text = month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            if (selectedDate != null) {
                HorizontalDivider()
                // Task list occupies 45% of the height in both modes
                Column(modifier = Modifier.weight(0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate!!.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = { onAddTaskClick(selectedDate!!) }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add Task", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (tasksOnSelectedDate.isEmpty()) {
                            item {
                                Text(
                                    text = "No tasks for this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(tasksOnSelectedDate.sortedBy { it.startTime }) { task ->
                                TaskSummaryItem(task, onClick = { onTaskClick(task) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskInCell(task: Task) {
    val timeStr = remember(task.startTime, task.isAllDay) {
        if (task.isAllDay) "" else {
            task.startTime?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it }
                String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } ?: ""
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .background(Color(task.color ?: 0).copy(alpha = 0.9f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (timeStr.isNotEmpty()) {
            Text(
                text = timeStr,
                fontSize = 6.5.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.wrapContentWidth()
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = task.title,
            fontSize = 7.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VerticalDayCell(day: CalendarDay, dayTasks: List<Task>, isSelected: Boolean, onClick: (CalendarDay) -> Unit) {
    val isToday = remember(day.date) { day.date == LocalDate.now() }
    
    Box(
        modifier = Modifier
            .aspectRatio(0.5f)
            .padding(1.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall
            )
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                else Modifier
            )
            .clickable { onClick(day) },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp)
            ) {
                dayTasks.take(4).forEach { task ->
                    TaskInCell(task)
                }
                if (dayTasks.size > 4) {
                    Text(
                        text = "+${dayTasks.size - 4}",
                        fontSize = 6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MonthHeader(month: com.kizitonwose.calendar.core.CalendarMonth, daysOfWeek: List<java.time.DayOfWeek>) {
    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            text = month.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dayOfWeek in daysOfWeek) {
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun Day(day: CalendarDay, isSelected: Boolean, dayTasks: List<Task>, onClick: (CalendarDay) -> Unit) {
    val isToday = remember(day.date) { day.date == LocalDate.now() }

    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .padding(1.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.extraSmall
            )
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                else Modifier
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) {
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                } else Color.LightGray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (day.position == DayPosition.MonthDate) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    dayTasks.take(3).forEach { task ->
                        TaskInCell(task)
                    }
                    if (dayTasks.size > 3) {
                        Text(
                            text = "+${dayTasks.size - 3}",
                            fontSize = 6.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSummaryItem(task: Task, onClick: () -> Unit) {
    val startCal = Calendar.getInstance().apply { timeInMillis = task.startTime ?: 0 }
    val endCal = Calendar.getInstance().apply { timeInMillis = task.endTime ?: 0 }
    
    val startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", 
        startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE))
    val endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", 
        endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(task.color ?: 0).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (task.isAllDay) {
                    Text(
                        text = "All day",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(text = startTimeStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(text = endTimeStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                
                if (!task.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            imageVector = Icons.Default.LocationOn, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = task.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(task.color ?: 0), MaterialTheme.shapes.extraSmall)
            )
        }
    }
}
