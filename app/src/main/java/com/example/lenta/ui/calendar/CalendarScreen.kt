package com.example.lenta.ui.calendar

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenta.model.Task
import com.example.lenta.ui.timeline.DeleteTaskConfirmationDialog
import com.example.lenta.ui.timeline.QuickAddTaskDialog
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
import java.util.Locale

enum class CalendarViewMode { MONTHLY, VERTICAL_LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    tasks: List<Task>,
    onAddTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    onViewModeChange: (CalendarViewMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    val tasksOnSelectedDate = remember(tasks, selectedDate) {
        tasks.filter { task ->
            task.startTime?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
            } ?: false
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
            IconButton(onClick = { 
                coroutineScope.launch {
                    state.scrollToMonth(YearMonth.now())
                    selectedDate = LocalDate.now()
                }
            }) {
                Text("🎯") // Today icon
            }
        }

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

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = { showAddDialog = true }, contentPadding = PaddingValues(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add Task", modifier = Modifier.padding(start = 4.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tasksOnSelectedDate.isEmpty()) {
                    item {
                        Text(
                            text = "No tasks for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(tasksOnSelectedDate) { task ->
                        TaskSummaryItem(task, onClick = { taskToDelete = task })
                    }
                }
            }
        } else {
            // Vertical Scroll Mode (Vertical Calendar)
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
                            onClick = { 
                                selectedDate = it.date
                                showBottomSheet = true
                            }
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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tasksOnSelectedDate.isEmpty()) {
                        item {
                            Text(
                                text = "No tasks for this day",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                            )
                        }
                    } else {
                        items(tasksOnSelectedDate) { task ->
                            TaskSummaryItem(task, onClick = { taskToDelete = task })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddDialog) {
        QuickAddTaskDialog(
            initialDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddTask(it)
                showAddDialog = false
            }
        )
    }

    if (taskToDelete != null) {
        DeleteTaskConfirmationDialog(
            task = taskToDelete!!,
            onDismiss = { taskToDelete = null },
            onConfirm = {
                onDeleteTask(taskToDelete!!)
                taskToDelete = null
            }
        )
    }
}

@Composable
fun VerticalDayCell(day: CalendarDay, dayTasks: List<Task>, isSelected: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.5f)
            .padding(1.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall
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
                    Text(
                        text = if (task.title.length > 9) task.title.take(8) + "…" else task.title,
                        fontSize = 7.sp,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(task.color ?: 0).copy(alpha = 0.9f), MaterialTheme.shapes.extraSmall)
                            .padding(vertical = 1.dp)
                    )
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
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) {
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                } else Color.LightGray
            )
            if (dayTasks.isNotEmpty() && day.position == DayPosition.MonthDate) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    dayTasks.take(4).forEach { task ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .padding(horizontal = 0.5.dp)
                                .background(Color(task.color ?: 0), MaterialTheme.shapes.extraSmall)
                        )
                    }
                    if (dayTasks.size > 4) {
                        Text(".", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSummaryItem(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(task.color ?: 0).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(task.color ?: 0), MaterialTheme.shapes.extraSmall)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = task.title, style = MaterialTheme.typography.titleSmall)
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
