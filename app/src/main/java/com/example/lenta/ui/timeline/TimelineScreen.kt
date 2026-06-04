package com.example.lenta.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenta.model.Task
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class ViewMode { TIMELINE, LIST }
enum class TimelineDays(val days: Int) { DAY_1(1), DAY_3(3), WEEK(7), WEEK_2(14) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    tasks: List<Task>,
    onAddTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    viewMode: ViewMode = ViewMode.TIMELINE,
    onViewModeChange: (ViewMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineTasks = remember(tasks) { tasks.filter { !it.isEasyModeEntry } }
    
    var scale by remember { mutableStateOf(1f) }
    var timelineDays by remember { mutableStateOf(TimelineDays.DAY_1) }
    
    val hourWidth = 100.dp * scale
    val scrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDaysMenu by remember { mutableStateOf(false) }

    val zoomState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 10f)
    }

    val taskPositions = remember(timelineTasks) { calculateTaskPositions(timelineTasks) }
    val laneHeight = 80.dp
    val maxLane = taskPositions.maxOfOrNull { it.lane } ?: 0
    val totalContentHeight = 100.dp + (laneHeight * (maxLane + 1)) // Extra padding for the bottom

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simplified toggle button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { 
                            onViewModeChange(if (viewMode == ViewMode.TIMELINE) ViewMode.LIST else ViewMode.TIMELINE)
                        }
                    ) {
                        Text(
                            text = if (viewMode == ViewMode.TIMELINE) "TIMELINE" else "TASK LIST",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (viewMode == ViewMode.TIMELINE) {
                        Box {
                            TextButton(onClick = { showDaysMenu = true }) {
                                Text(text = when(timelineDays) {
                                    TimelineDays.DAY_1 -> "1 Day"
                                    TimelineDays.DAY_3 -> "3 Days"
                                    TimelineDays.WEEK -> "1 Week"
                                    TimelineDays.WEEK_2 -> "2 Weeks"
                                }, style = MaterialTheme.typography.labelLarge)
                            }
                            DropdownMenu(expanded = showDaysMenu, onDismissRequest = { showDaysMenu = false }) {
                                TimelineDays.entries.forEach { daysOption ->
                                    DropdownMenuItem(
                                        text = { Text(daysOption.name.replace("_", " ").replace("DAY ", "")) },
                                        onClick = {
                                            timelineDays = daysOption
                                            showDaysMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Button(onClick = { showAddDialog = true }) {
                    Text("+")
                }
            }

            if (viewMode == ViewMode.TIMELINE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .horizontalScroll(scrollState)
                            .verticalScroll(verticalScrollState)
                            .transformable(state = zoomState, lockRotationOnZoomPan = true)
                    ) {
                        TimelineGrid(hourWidth, scale, timelineDays.days, totalContentHeight)
                        TimelineTasks(timelineTasks, hourWidth, timelineDays.days, taskPositions, totalContentHeight) { task ->
                            taskToDelete = task
                        }
                        CurrentTimeLine(hourWidth, totalContentHeight)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "Scale: %.1fx", scale),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxScroll = scrollState.maxValue.toFloat()
                        if (maxScroll > 0) {
                            Slider(
                                value = scrollState.value.toFloat(),
                                onValueChange = { newValue ->
                                    coroutineScope.launch {
                                        scrollState.scrollTo(newValue.toInt())
                                    }
                                },
                                valueRange = 0f..maxScroll
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timelineTasks.sortedBy { it.startTime ?: 0L }) { task ->
                        TaskListItem(task, onClick = { taskToDelete = task })
                    }
                }
            }
        }

        if (showAddDialog) {
            QuickAddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { task ->
                    onAddTask(task)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskDialog(
    initialDate: java.time.LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val baseCal = Calendar.getInstance().apply {
        initialDate?.let {
            set(Calendar.YEAR, it.year)
            set(Calendar.MONTH, it.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, it.dayOfMonth)
        }
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    var startTime by remember { mutableStateOf(baseCal.timeInMillis) }
    var endTime by remember { mutableStateOf((baseCal.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }.timeInMillis) }
    var selectedColor by remember { mutableIntStateOf(Color.Blue.toArgb()) }
    
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray)

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.MINUTE),
        is24Hour = true
    )
    val endState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = endTime }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = endTime }.get(Calendar.MINUTE),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Task") },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Start: ", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showStartTimePicker = true }) {
                        val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                        Text(String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End: ", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showEndTimePicker = true }) {
                        val cal = Calendar.getInstance().apply { timeInMillis = endTime }
                        Text(String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Color:", style = MaterialTheme.typography.bodyMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, MaterialTheme.shapes.small)
                                .clickable { selectedColor = color.toArgb() }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == color.toArgb()) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val startH = startState.hour.toFloat() + startState.minute / 60f
                var endH = endState.hour.toFloat() + endState.minute / 60f
                
                if (endH < startH) {
                    if (endH < 12) endH += 12
                }
                
                val actualStart = minOf(startH, endH)
                val actualEnd = maxOf(startH, endH)
                
                val finalStartCal = (baseCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, actualStart.toInt())
                    set(Calendar.MINUTE, ((actualStart % 1) * 60).toInt())
                }
                val finalEndCal = (baseCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, actualEnd.toInt())
                    set(Calendar.MINUTE, ((actualEnd % 1) * 60).toInt())
                }

                onConfirm(Task(
                    title = title.ifBlank { "New Task" },
                    description = description,
                    startTime = finalStartCal.timeInMillis,
                    endTime = finalEndCal.timeInMillis,
                    color = selectedColor
                ))
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startTime
                        set(Calendar.HOUR_OF_DAY, startState.hour)
                        set(Calendar.MINUTE, startState.minute)
                    }
                    startTime = cal.timeInMillis
                    showStartTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = startState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = endTime
                        set(Calendar.HOUR_OF_DAY, endState.hour)
                        set(Calendar.MINUTE, endState.minute)
                    }
                    endTime = cal.timeInMillis
                    showEndTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = endState)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } },
        text = { content() }
    )
}

@Composable
fun TaskListItem(task: Task, onClick: () -> Unit) {
    val startCal = Calendar.getInstance().apply { timeInMillis = task.startTime ?: 0 }
    val endCal = Calendar.getInstance().apply { timeInMillis = task.endTime ?: 0 }
    val timeText = if (task.startTime != null) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d - %02d:%02d", 
            startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE),
            endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))
    } else "No time"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(task.color ?: MaterialTheme.colorScheme.surface.toArgb()).copy(alpha = 0.2f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(Color(task.color ?: 0), MaterialTheme.shapes.extraSmall))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(timeText, style = MaterialTheme.typography.bodySmall)
                if (task.description.isNotBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun DeleteTaskConfirmationDialog(task: Task, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Task") },
        text = { Text("Delete '${task.title}'?") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TimelineGrid(hourWidth: androidx.compose.ui.unit.Dp, scale: Float, days: Int = 1, height: androidx.compose.ui.unit.Dp) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val totalHours = 24 * days
    val totalWidth = hourWidth * totalHours
    Box(modifier = Modifier.height(height).width(totalWidth)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hourWidthPx = hourWidth.toPx()
            
            for (i in 0..totalHours) {
                val x = i * hourWidthPx
                // Hour lines
                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                
                // 30 minute lines (>1.8x)
                if (scale >= 0.8f) { // Logic for drawing lines should stay consistent, label logic changes
                    val midX = x + hourWidthPx / 2
                    if (i < totalHours) {
                        drawLine(
                            color = gridColor.copy(alpha = 0.7f),
                            start = Offset(midX, 0f),
                            end = Offset(midX, size.height),
                            strokeWidth = 0.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                        )
                    }
                }
                
                // 15 minute lines (>1.5x)
                if (scale >= 1.5f) {
                    val q1X = x + hourWidthPx / 4
                    val q3X = x + 3 * hourWidthPx / 4
                    if (i < totalHours) {
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(q1X, 0f),
                            end = Offset(q1X, size.height),
                            strokeWidth = 0.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(q3X, 0f),
                            end = Offset(q3X, size.height),
                            strokeWidth = 0.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until totalHours) {
                val hourOfDay = i % 24
                Box(modifier = Modifier.width(hourWidth)) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d:00", hourOfDay), 
                        fontSize = 12.sp, 
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (scale >= 1.8f) {
                        Text(
                            text = String.format(java.util.Locale.getDefault(), "%02d:30", hourOfDay), 
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .offset(x = hourWidth / 2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTasks(
    tasks: List<Task>, 
    hourWidth: androidx.compose.ui.unit.Dp, 
    days: Int = 1, 
    taskPositions: List<TaskPosition>,
    height: androidx.compose.ui.unit.Dp,
    onTaskClick: (Task) -> Unit
) {
    // Filter tasks within the range of days from today
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val endOfRange = startOfToday + (days * 24 * 60 * 60 * 1000L)

    val laneHeight = 80.dp
    
    Box(modifier = Modifier.height(height).wrapContentHeight(Alignment.Top)) {
        taskPositions.forEach { position ->
            val task = position.task
            val taskStart = task.startTime ?: 0L
            if (taskStart >= startOfToday && taskStart < endOfRange) {
                val hoursFromStartOfToday = (taskStart - startOfToday).toFloat() / (60 * 60 * 1000f)
                val durationHours = ((task.endTime ?: taskStart) - taskStart).toFloat() / (60 * 60 * 1000f)
                
                val left = hourWidth * hoursFromStartOfToday
                val width = hourWidth * durationHours
                val topOffset = 40.dp + (laneHeight * position.lane)
                
                Surface(
                    modifier = Modifier
                        .offset(x = left, y = topOffset)
                        .width(maxOf(width, 10.dp)) // Minimum width for visibility
                        .height(laneHeight - 4.dp)
                        .pointerInput(task) { detectTapGestures { onTaskClick(task) } },
                    color = Color(task.color ?: MaterialTheme.colorScheme.primary.toArgb()),
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = task.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                        if (width > 80.dp) {
                            Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

private fun getHourFromMillis(millis: Long): Float {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
}

@Composable
fun CurrentTimeLine(hourWidth: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
    val x = hourWidth * hour
    Box(modifier = Modifier.offset(x = x).height(height).width(2.dp).background(Color.Red))
}
