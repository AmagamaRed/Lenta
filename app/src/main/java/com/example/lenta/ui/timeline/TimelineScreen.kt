package com.example.lenta.ui.timeline

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import com.example.lenta.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class ViewMode { TIMELINE, LIST }
enum class TimelineDays(val days: Int) { DAY_1(1), DAY_3(3), DAY_5(5), WEEK(7) }

data class DayConfig(
    val startMillis: Long,
    val taskPositions: List<TaskPosition>,
    val height: androidx.compose.ui.unit.Dp,
    val yOffset: androidx.compose.ui.unit.Dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onAddTaskClick: (Long?) -> Unit,
    viewMode: ViewMode = ViewMode.TIMELINE,
    onViewModeChange: (ViewMode) -> Unit = {},
    laneScale: Float = 1f,
    lockVerticalScroll: Boolean = false,
    customMinX: Float = -15000f,
    customMaxX: Float = 15000f,
    initialTimelineDays: TimelineDays = TimelineDays.DAY_3,
    onTimelineDaysChange: (TimelineDays) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timelineTasks = remember(tasks) { tasks.filter { !it.isEasyModeEntry } }

    var scale by rememberSaveable { mutableStateOf(1f) }
    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(if (lockVerticalScroll) 80f else 0f) }
    var timelineDays by rememberSaveable { mutableStateOf(initialTimelineDays) }
    var showDaysMenu by remember { mutableStateOf(false) }

    val offsetXAnim = remember { Animatable(offsetX) }
    val offsetYAnim = remember { Animatable(offsetY) }

    LaunchedEffect(offsetX) { if (!offsetXAnim.isRunning) offsetXAnim.snapTo(offsetX) }
    LaunchedEffect(offsetY) { if (!offsetYAnim.isRunning) offsetYAnim.snapTo(offsetY) }

    LaunchedEffect(initialTimelineDays) {
        timelineDays = initialTimelineDays
    }

    val baseHourWidth = 100.dp
    val laneHeight = (if (timelineDays == TimelineDays.DAY_1) 160.dp else 80.dp) * laneScale
    
    // ЭТОТ ПАРАМЕТР (X) КОНТРОЛИРУЕТ СДВИГ ВРЕМЕНИ И ЭКРАНА
    // Установите здесь нужное значение (например, 640 для 10ч 40мин)
    val manualTimeShiftMinutes = 0

    val timelineStart = remember(timelineDays, lockVerticalScroll) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (timelineDays == TimelineDays.WEEK && !lockVerticalScroll) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = (dayOfWeek + 5) % 7 // Пн(2)->0, Вт(3)->1 ... Вс(1)->6
            cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        }
        cal.timeInMillis
    }

    val visibleRangeEnd = timelineStart + (timelineDays.days * 24 * 60 * 60 * 1000L)

    val tasksInRange = remember(timelineTasks, timelineStart, visibleRangeEnd) {
        timelineTasks.filter {
            val start = it.startTime ?: 0L
            val end = it.endTime ?: start
            start < visibleRangeEnd && end > timelineStart
        }
    }
    val globalTaskPositions = remember(tasksInRange) { calculateTaskPositions(tasksInRange) }

    val dayConfigs = remember<List<DayConfig>>(globalTaskPositions, timelineDays, laneHeight, timelineStart) {
        var currentY = 0.dp
        List(timelineDays.days) { dayIndex ->
            val dayStart = timelineStart + (dayIndex * 24 * 60 * 60 * 1000L)
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)

            val dayPositions = globalTaskPositions.filter { pos ->
                val mainStart = pos.task.startTime ?: 0L
                val mainEnd = pos.task.endTime ?: mainStart
                
                val travelStart = mainStart - pos.task.travelTimeBeforeMs
                val travelEnd = mainEnd + pos.task.travelTimeAfterMs

                if (pos.task.isAllDay) {
                    // For All Day tasks, check if the task's start time falls within this day
                    mainStart >= dayStart && mainStart < dayEnd
                } else {
                    travelStart < dayEnd && travelEnd > dayStart
                }
            }

            val dayMaxLane = dayPositions.maxOfOrNull { it.lane } ?: 0
            val globalAllDayCount = globalTaskPositions.count { it.task.isAllDay }
            val timedLanesCount = if (dayPositions.isEmpty()) 1 else maxOf(0, dayMaxLane - globalAllDayCount + 1)
            val dayHeight = 40.dp + (36.dp * globalAllDayCount) + (laneHeight * timedLanesCount) + 60.dp

            val config = DayConfig(
                startMillis = dayStart,
                taskPositions = dayPositions,
                height = dayHeight,
                yOffset = currentY
            )

            currentY += dayHeight + 20.dp
            config
        }
    }

    val totalContentHeight = if (dayConfigs.isEmpty()) 0.dp else {
        val last = dayConfigs.last()
        last.yOffset + last.height
    }
    val totalWidth = baseHourWidth * 24

    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            tick++
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current

        var isFirstLayout by remember { mutableStateOf(true) }
        LaunchedEffect(viewportWidthPx) {
            if (isFirstLayout && viewportWidthPx > 0) {
                val cal = Calendar.getInstance()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
                val hourWidthPx = with(density) { baseHourWidth.toPx() }

                // УЧИТЫВАЕМ СДВИГ ПРИ ЦЕНТРИРОВАНИИ
                val markerContentX = (currentHour + (manualTimeShiftMinutes / 60f)) * hourWidthPx
                val targetOffsetX = (viewportWidthPx / (2f * scale)) - markerContentX
                
                offsetX = targetOffsetX.coerceIn(customMinX, customMaxX)
                offsetXAnim.snapTo(offsetX)
                isFirstLayout = false
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                                    TimelineDays.DAY_5 -> "5 Days"
                                    TimelineDays.WEEK -> "1 Week"
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

                Button(onClick = { onAddTaskClick(null) }) {
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
                        .pointerInput(dayConfigs, scale, offsetX, offsetY) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    val currentScale = scale
                                    val currentOffsetX = offsetX
                                    val currentOffsetY = offsetY
                                    
                                    val contentX = (offset.x / currentScale) - currentOffsetX
                                    val contentY = (offset.y / currentScale) - currentOffsetY
                                    
                                    val hourWidthPx = baseHourWidth.toPx()
                                    
                                    val targetConfig = dayConfigs.find { config ->
                                        val yStart = config.yOffset.toPx()
                                        val yEnd = yStart + config.height.toPx()
                                        contentY >= yStart && contentY <= yEnd
                                    }
                                    
                                    targetConfig?.let { config ->
                                        val hourDecimal = contentX.toDouble() / hourWidthPx.toDouble()
                                        
                                        val cal = Calendar.getInstance().apply { 
                                            timeInMillis = config.startMillis
                                            
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                            
                                            // 1. Считаем минуты от начала дня (ВЫЧИТАЕМ любые сдвиги, если они есть)
                                            val baseMinutes = (hourDecimal * 60.0).roundToInt()
                                            val totalMinutesWithOffset = baseMinutes - manualTimeShiftMinutes
                                            
                                            // 2. Округляем до ближайших 30 минут (13:45 -> 14:00, 13:44 -> 13:30)
                                            val roundedMinutes = ((totalMinutesWithOffset + 15) / 30) * 30
                                            
                                            // 3. Устанавливаем итоговое время
                                            add(Calendar.MINUTE, roundedMinutes)
                                        }
                                        onAddTaskClick(cal.timeInMillis)
                                    }
                                }
                            )
                        }
                        .pointerInput(lockVerticalScroll, customMinX, customMaxX) {
                            coroutineScope {
                                awaitEachGesture {
                                    val velocityTracker = VelocityTracker()
                                    var isMultiTouch = false
                                    awaitFirstDown()

                                    launch { offsetXAnim.stop() }
                                    launch { offsetYAnim.stop() }

                                    var zoom = 1f
                                    var pan = Offset.Zero
                                    var pastTouchSlop = false
                                    val touchSlop = viewConfiguration.touchSlop

                                    do {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) isMultiTouch = true

                                        val canceled = event.changes.any { it.isConsumed }
                                        if (!canceled) {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()

                                            if (!pastTouchSlop) {
                                                zoom *= zoomChange
                                                pan += panChange
                                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                val zoomMotion = abs(1 - zoom) * centroidSize
                                                val panMotion = pan.getDistance()

                                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                                    pastTouchSlop = true
                                                }
                                            }

                                            if (pastTouchSlop) {
                                                val centroid = event.calculateCentroid(useCurrent = false)

                                                if (zoomChange != 1f) {
                                                    val oldScale = scale
                                                    scale = (scale * zoomChange).coerceIn(0.2f, 5f)

                                                    val dx = centroid.x * (1f / scale - 1f / oldScale)
                                                    val dy = centroid.y * (1f / scale - 1f / oldScale)

                                                    val nextX = offsetX + dx
                                                    offsetX = if (lockVerticalScroll) nextX.coerceIn(customMinX, customMaxX) else nextX
                                                    offsetY += dy
                                                }
                                                if (panChange != Offset.Zero) {
                                                    val nextPanX = offsetX + (panChange.x / scale)
                                                    offsetX = if (lockVerticalScroll) nextPanX.coerceIn(customMinX, customMaxX) else nextPanX
                                                    if (!lockVerticalScroll) {
                                                        offsetY += panChange.y / scale
                                                    }
                                                }

                                                event.changes.forEach {
                                                    velocityTracker.addPosition(it.uptimeMillis, it.position)
                                                    if (it.position != it.previousPosition) it.consume()
                                                }
                                            }
                                        }
                                    } while (!canceled && event.changes.any { it.pressed })

                                    if (lockVerticalScroll && !isMultiTouch) {
                                        val velocity = velocityTracker.calculateVelocity()
                                        val absVelX = abs(velocity.x)

                                        if (absVelX > 200f) {
                                            val boostFactor = when {
                                                absVelX > 5000f -> 5.5f
                                                absVelX > 2500f -> 4.0f
                                                absVelX > 1000f -> 2.5f
                                                else -> 1.5f
                                            }
                                            val finalVelocityX = (velocity.x * boostFactor) / scale
                                            val decay = exponentialDecay<Float>(frictionMultiplier = 1.1f)

                                            launch {
                                                offsetXAnim.snapTo(offsetX)
                                                offsetXAnim.animateDecay(finalVelocityX, decay) {
                                                    if (value < customMinX || value > customMaxX) {
                                                        offsetX = value.coerceIn(customMinX, customMaxX)
                                                        launch { offsetXAnim.stop() }
                                                    } else {
                                                        offsetX = value
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX * scale,
                                translationY = offsetY * scale,
                                transformOrigin = TransformOrigin(0f, 0f)
                            )
                            .width(totalWidth)
                            .height(totalContentHeight)
                    ) {
                        val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

                        dayConfigs.forEachIndexed { dayIndex, config ->
                            Box(
                                modifier = Modifier
                                    .offset(y = config.yOffset)
                                    .requiredWidth(totalWidth)
                                    .height(config.height)
                                    .clipToBounds()
                            ) {
                                val cal = remember(config.startMillis) {
                                    Calendar.getInstance().apply { timeInMillis = config.startMillis }
                                }
                                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                                val dayNum = if (dayOfWeek == 1) 7 else dayOfWeek - 1
                                val dayName = when(dayNum) {
                                    1 -> "Пн"
                                    2 -> "Вт"
                                    3 -> "Ср"
                                    4 -> "Чт"
                                    5 -> "Пт"
                                    6 -> "Сб"
                                    7 -> "Вс"
                                    else -> ""
                                }

                                TimelineGrid(baseHourWidth, scale, 1, config.height)

                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        text = "$dayNum. $dayName",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = if (dayNum == 7) Color.Red else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .graphicsLayer(alpha = 0.3f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(start = baseHourWidth * 6),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(config.startMillis)),
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.graphicsLayer(alpha = 0.15f),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(start = baseHourWidth * 12),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(config.startMillis)),
                                        style = MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier.graphicsLayer(alpha = 0.15f),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TimelineTasks(
                                    tasks = emptyList(), // parameter not used in implementation
                                    baseHourWidth = baseHourWidth,
                                    dayStartMillis = config.startMillis,
                                    taskPositions = config.taskPositions,
                                    height = config.height,
                                    laneHeight = laneHeight,
                                    timedLaneOffset = globalTaskPositions.count { it.task.isAllDay }
                                ) { task ->
                                    onTaskClick(task)
                                }

                                val isToday = remember(config.startMillis) {
                                    val now = Calendar.getInstance()
                                    val day = Calendar.getInstance().apply { timeInMillis = config.startMillis }
                                    now.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                            now.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                                }

                                if (isToday) {
                                    CurrentTimeLine(baseHourWidth, config.height, tick, manualTimeShiftMinutes)
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
                    items(timelineTasks.filter { !it.isVisibleOnlyOnTimeline }.sortedBy { it.startTime ?: 0L }) { task ->
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
    val timeText = if (task.isAllDay) {
        "All day"
    } else if (task.startTime != null) {
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
                if (!task.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(task.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
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
            drawRect(color = backgroundColor, size = size)
            val hourWidthPx = baseHourWidth.toPx()
            for (i in 0..totalHours) {
                val x = i * hourWidthPx
                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                if (scale >= 0.8f) {
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
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).offset(x = baseHourWidth / 2)
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
    timedLaneOffset: Int,
    onTaskClick: (Task) -> Unit
) {
    val endOfRange = dayStartMillis + (24 * 60 * 60 * 1000L)
    Box(modifier = Modifier.height(height).wrapContentHeight(Alignment.Top)) {
        taskPositions.forEach { position ->
            val task = position.task
            val taskStart = task.startTime ?: 0L
            val taskEnd = task.endTime ?: taskStart

            val isVisibleOnThisDay = if (task.isAllDay) {
                taskStart >= dayStartMillis && taskStart < endOfRange
            } else {
                taskStart < endOfRange && taskEnd > dayStartMillis
            }

            if (isVisibleOnThisDay) {
                val effectiveStart = if (task.isAllDay) dayStartMillis else maxOf(dayStartMillis, taskStart)
                val effectiveEnd = if (task.isAllDay) endOfRange else minOf(endOfRange, taskEnd)

                val left = baseHourWidth * ((effectiveStart - dayStartMillis).toFloat() / (60 * 60 * 1000f))
                val width = baseHourWidth * ((effectiveEnd - effectiveStart).toFloat() / (60 * 60 * 1000f))

                val topOffset = if (task.isAllDay) {
                    40.dp + (36.dp * position.lane)
                } else {
                    40.dp + (36.dp * timedLaneOffset) + (laneHeight * (position.lane - timedLaneOffset))
                }

                val taskHeight = if (task.isAllDay) 32.dp else 76.dp

                Surface(
                    modifier = Modifier
                        .offset(x = left, y = topOffset)
                        .width(maxOf(width, 10.dp))
                        .height(taskHeight)
                        .clickable { onTaskClick(task) },
                    color = Color(task.color ?: MaterialTheme.colorScheme.primary.toArgb()).copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = task.title,
                            style = (if (task.isAllDay) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium).copy(fontFamily = FontFamily.SansSerif, fontSize = 18.sp),
                            color = Color.White,
                            maxLines = if (task.isAllDay) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!task.isAllDay) {
                            if (!task.location.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(12.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = task.location,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!task.isAllDay) {
                    val travelColor = Color(task.color ?: MaterialTheme.colorScheme.primary.toArgb()).copy(alpha = 0.35f)
                    if (task.travelTimeBeforeMs > 0) {
                        val beforeWidth = baseHourWidth * (task.travelTimeBeforeMs.toFloat() / 3600000f)
                        val hours = task.travelTimeBeforeMs / 3600000
                        val mins = (task.travelTimeBeforeMs % 3600000) / 60000
                        val travelText = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"
                        Surface(
                            modifier = Modifier.offset(x = left - beforeWidth, y = topOffset).width(beforeWidth).height(taskHeight),
                            color = travelColor,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = travelText, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, fontSize = 8.sp)
                            }
                        }
                    }
                    if (task.travelTimeAfterMs > 0) {
                        val afterWidth = baseHourWidth * (task.travelTimeAfterMs.toFloat() / 3600000f)
                        val hours = task.travelTimeAfterMs / 3600000
                        val mins = (task.travelTimeAfterMs % 3600000) / 60000
                        val travelText = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"
                        Surface(
                            modifier = Modifier.offset(x = left + width, y = topOffset).width(afterWidth).height(taskHeight),
                            color = travelColor,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = travelText, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTimeLine(baseHourWidth: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, tick: Int, shiftMinutes: Int = 0) {
    val now = remember(tick) { Calendar.getInstance() }
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)
    
    // УЧИТЫВАЕМ СДВИГ ДЛЯ КРАСНОЙ ЛИНИИ
    val timeOffset = baseHourWidth * (hour + (minute + shiftMinutes) / 60f)
    Box(modifier = Modifier.fillMaxHeight().width(2.dp).offset(x = timeOffset).background(Color.Red)) {
        Box(modifier = Modifier.size(8.dp).align(Alignment.TopCenter).offset(y = (-4).dp).background(Color.Red, shape = CircleShape))
    }
}
