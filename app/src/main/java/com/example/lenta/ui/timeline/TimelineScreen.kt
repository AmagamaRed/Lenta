package com.example.lenta.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import com.example.lenta.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

enum class ViewMode { TIMELINE, LIST }
enum class TimelineDays(val days: Int) { DAY_1(1), DAY_3(3), WEEK(7), WEEK_2(14) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onAddTaskClick: () -> Unit,
    viewMode: ViewMode = ViewMode.TIMELINE,
    onViewModeChange: (ViewMode) -> Unit = {},
    laneScale: Float = 1f,
    stickTimelines: Boolean = false,
    initialTimelineDays: TimelineDays = TimelineDays.DAY_1,
    onTimelineDaysChange: (TimelineDays) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineTasks = remember(tasks) { tasks.filter { !it.isEasyModeEntry } }
    
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var timelineDays by remember { mutableStateOf(initialTimelineDays) }
    var showDaysMenu by remember { mutableStateOf(false) }

    // Sync internal state with external when external changes
    LaunchedEffect(initialTimelineDays) {
        timelineDays = initialTimelineDays
    }
    
    val baseHourWidth = 100.dp
    val laneHeight = 80.dp * laneScale

    val taskPositions = remember(timelineTasks) { calculateTaskPositions(timelineTasks) }
    val maxLane = taskPositions.maxOfOrNull { it.lane } ?: 0
    val singleDayHeight = 100.dp + (laneHeight * (maxLane + 1))
    val totalContentHeight = if (stickTimelines) {
        singleDayHeight * timelineDays.days
    } else {
        (singleDayHeight + 20.dp) * timelineDays.days
    }
    val totalWidth = baseHourWidth * 24

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
                                            onTimelineDaysChange(daysOption)
                                            showDaysMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Button(onClick = onAddTaskClick) {
                    Text("+")
                }
            }

            if (viewMode == ViewMode.TIMELINE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = scale
                                scale = (scale * zoom).coerceIn(0.2f, 5f)

                                val actualZoom = scale / oldScale
                                offset = (offset * actualZoom) + pan - (centroid * (actualZoom - 1f))
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                transformOrigin = TransformOrigin(0f, 0f)
                            )
                            .width(totalWidth)
                            .height(totalContentHeight)
                    ) {
                        val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
                        val startOfToday = remember {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }

                        for (dayIndex in 0 until timelineDays.days) {
                            val dayStartMillis = startOfToday + (dayIndex * 24 * 60 * 60 * 1000L)
                            val dayEndMillis = dayStartMillis + (24 * 60 * 60 * 1000L)
                            val dayYOffset = if (stickTimelines) {
                                singleDayHeight * dayIndex
                            } else {
                                (singleDayHeight + 20.dp) * dayIndex
                            }

                            Box(
                                modifier = Modifier
                                    .offset(y = dayYOffset)
                                    .requiredWidth(totalWidth) // Ensure it doesn't get squeezed
                                    .height(singleDayHeight)
                                    .clipToBounds()
                            ) {
                                TimelineGrid(baseHourWidth, scale, 1, singleDayHeight)

                                // Date label: Centered in the middle of the day's timeline area
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dateFormat.format(Date(dayStartMillis)),
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.graphicsLayer(alpha = 0.15f), // Watermark effect
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val dayTasks = timelineTasks.filter {
                                    val start = it.startTime ?: 0L
                                    start >= dayStartMillis && start < dayEndMillis
                                }
                                // We use the same taskPositions but filter them, 
                                // then we'll need to adjust their horizontal pos in TimelineTasks
                                val dayTaskPositions = taskPositions.filter {
                                    val start = it.task.startTime ?: 0L
                                    start >= dayStartMillis && start < dayEndMillis
                                }

                                TimelineTasks(
                                    tasks = dayTasks,
                                    baseHourWidth = baseHourWidth,
                                    dayStartMillis = dayStartMillis,
                                    taskPositions = dayTaskPositions,
                                    height = singleDayHeight,
                                    laneHeight = laneHeight
                                ) { task ->
                                    onTaskClick(task)
                                }

                                if (dayIndex == 0) {
                                    CurrentTimeLine(baseHourWidth, singleDayHeight)
                                }
                            }
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
                        TaskListItem(task, onClick = { onTaskClick(task) })
                    }
                }
            }
        }
    }
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
fun TimelineGrid(baseHourWidth: androidx.compose.ui.unit.Dp, scale: Float, days: Int = 1, height: androidx.compose.ui.unit.Dp) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = MaterialTheme.colorScheme.background
    val totalHours = 24 * days
    val totalWidth = baseHourWidth * totalHours
    Box(modifier = Modifier.height(height).width(totalWidth)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Отрисовка фона на весь холст с запасом под масштаб
            val extendedWidth = totalWidth.toPx() * (1f / scale)
            drawRect(
                color = backgroundColor,
                size = androidx.compose.ui.geometry.Size(extendedWidth, size.height)
            )

            val hourWidthPx = baseHourWidth.toPx()

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
        Box(modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until totalHours) {
                val hourOfDay = i % 24
                val xOffset = baseHourWidth * i
                Box(modifier = Modifier.offset(x = xOffset).width(baseHourWidth)) {
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
                                .offset(x = baseHourWidth / 2)
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
    baseHourWidth: androidx.compose.ui.unit.Dp,
    dayStartMillis: Long,
    taskPositions: List<TaskPosition>,
    height: androidx.compose.ui.unit.Dp,
    laneHeight: androidx.compose.ui.unit.Dp = 80.dp,
    onTaskClick: (Task) -> Unit
) {
    val endOfRange = dayStartMillis + (24 * 60 * 60 * 1000L)

    Box(modifier = Modifier.height(height).wrapContentHeight(Alignment.Top)) {
        taskPositions.forEach { position ->
            val task = position.task
            val taskStart = task.startTime ?: 0L
            if (taskStart >= dayStartMillis && taskStart < endOfRange) {
                val hoursFromStartOfDay = (taskStart - dayStartMillis).toFloat() / (60 * 60 * 1000f)
                val durationHours = ((task.endTime ?: taskStart) - taskStart).toFloat() / (60 * 60 * 1000f)

                val left = baseHourWidth * hoursFromStartOfDay
                val width = baseHourWidth * durationHours
                val topOffset = 40.dp + (laneHeight * position.lane)

                Surface(
                    modifier = Modifier
                        .offset(x = left, y = topOffset)
                        .width(maxOf(width, 10.dp))
                        // Task height remains static (76dp) as requested, 
                        // while laneHeight (spacing) changes with settings
                        .height(76.dp)
                        .pointerInput(task) { detectTapGestures { onTaskClick(task) } },
                    color = Color(task.color ?: MaterialTheme.colorScheme.primary.toArgb()),
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = task.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)

                        if (!task.location.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = task.location,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }

                        if (width > 80.dp && task.description.isNotBlank()) {
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
fun CurrentTimeLine(baseHourWidth: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f
    val x = baseHourWidth * hour
    Box(modifier = Modifier.offset(x = x).height(height).width(2.dp).background(Color.Red))
}
