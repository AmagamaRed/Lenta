package com.example.lenta.ui.timeline



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

    onAddTaskClick: () -> Unit,

    viewMode: ViewMode = ViewMode.TIMELINE,

    onViewModeChange: (ViewMode) -> Unit = {},

    laneScale: Float = 1f,

    stickTimelines: Boolean = false,

    lockVerticalScroll: Boolean = false,

    customMinX: Float = -3000f,

    customMaxX: Float = 3500f,

    initialTimelineDays: TimelineDays = TimelineDays.DAY_3,

    onTimelineDaysChange: (TimelineDays) -> Unit = {},

    modifier: Modifier = Modifier

) {

    val timelineTasks = remember(tasks) { tasks.filter { !it.isEasyModeEntry } }


    var scale by rememberSaveable { mutableStateOf(1f) }

    var offsetX by rememberSaveable { mutableStateOf(0f) }

// Инициализируем смещением вниз, чтобы лента была выше центра, но не впритык

    var offsetY by rememberSaveable { mutableStateOf(if (lockVerticalScroll) 80f else 0f) }

    var timelineDays by rememberSaveable { mutableStateOf(initialTimelineDays) }

    var showDaysMenu by remember { mutableStateOf(false) }


    val offsetXAnim = remember { Animatable(offsetX) }

    val offsetYAnim = remember { Animatable(offsetY) }


// Sync animatable with state changes from outside (if any) or manual updates

    LaunchedEffect(offsetX) { if (!offsetXAnim.isRunning) offsetXAnim.snapTo(offsetX) }

    LaunchedEffect(offsetY) { if (!offsetYAnim.isRunning) offsetYAnim.snapTo(offsetY) }



// Sync internal state with external when external changes

    LaunchedEffect(initialTimelineDays) {

        timelineDays = initialTimelineDays

    }


    val baseHourWidth = 100.dp

    val laneHeight = (if (timelineDays == TimelineDays.DAY_1) 160.dp else 80.dp) * laneScale



    val timelineStart = remember(timelineDays, lockVerticalScroll) {

        val cal = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, 0)

            set(Calendar.MINUTE, 0)

            set(Calendar.SECOND, 0)

            set(Calendar.MILLISECOND, 0)

        }

// Если это основная лента (без лока) и выбрана неделя - стартуем с Пн

        if (timelineDays == TimelineDays.WEEK && !lockVerticalScroll) {

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val daysToSubtract = (dayOfWeek + 5) % 7 // Пн(2)->0, Вт(3)->1 ... Вс(1)->6

            cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        }

        cal.timeInMillis

    }



    val visibleRangeEnd = timelineStart + (timelineDays.days * 24 * 60 * 60 * 1000L)



// Calculate lane positions for ALL tasks in the visible range to ensure consistency

    val tasksInRange = remember(timelineTasks, timelineStart, visibleRangeEnd) {

        timelineTasks.filter {

            val start = it.startTime ?: 0L

            val end = it.endTime ?: start

            start < visibleRangeEnd && end > timelineStart

        }

    }

    val globalTaskPositions = remember(tasksInRange) { calculateTaskPositions(tasksInRange) }



// Calculate per-day heights and positions

    val dayConfigs = remember<List<DayConfig>>(globalTaskPositions, timelineDays, laneHeight, stickTimelines, timelineStart) {

        var currentY = 0.dp

        List(timelineDays.days) { dayIndex ->

            val dayStart = timelineStart + (dayIndex * 24 * 60 * 60 * 1000L)

            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)


// Include tasks that overlap with this specific day (including travel time)

            val dayPositions = globalTaskPositions.filter { pos ->

                val mainStart = pos.task.startTime ?: 0L

                val mainEnd = pos.task.endTime ?: mainStart
                
                val travelStart = mainStart - pos.task.travelTimeBeforeMs
                val travelEnd = mainEnd + pos.task.travelTimeAfterMs


                pos.task.isAllDay || (travelStart < dayEnd && travelEnd > dayStart)

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


            currentY += dayHeight + if (stickTimelines) 0.dp else 20.dp

            config

        }

    }



    val totalContentHeight = if (dayConfigs.isEmpty()) 0.dp else {

        val last = dayConfigs.last()

        last.yOffset + last.height

    }

    val totalWidth = baseHourWidth * 24



    BoxWithConstraints(modifier = modifier.fillMaxSize()) {

        val viewportWidthPx = constraints.maxWidth.toFloat()

        val density = LocalDensity.current



// Автоматическое центрирование на текущем времени при первом запуске

        var isFirstLayout by remember { mutableStateOf(true) }

        LaunchedEffect(viewportWidthPx) {

            if (isFirstLayout) {

                val cal = Calendar.getInstance()

                val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f

                val hourWidthPx = with(density) { baseHourWidth.toPx() }


// Целевая позиция X для маркера в экранных координатах

                val targetMarkerX = viewportWidthPx / 2f


// ВычисляемoffsetX так, чтобы маркер (currentHour * hourWidthPx * scale)

// после применения трансформации (offsetX * scale) оказался в targetMarkerX

// Формула отрисовки: ScreenX = (offsetX + ContentX) * scale

// Отсюда: offsetX = (targetMarkerX / scale) - ContentX


                val markerContentX = currentHour * hourWidthPx

                offsetX = (targetMarkerX / scale) - markerContentX


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

                        .pointerInput(lockVerticalScroll, customMinX, customMaxX) {

                            val totalWidthPx = totalWidth.toPx()


                            coroutineScope {

                                awaitEachGesture {

                                    val velocityTracker = VelocityTracker()

                                    var isMultiTouch = false

                                    awaitFirstDown()


// Stop any ongoing fling

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


// Центрирование зума на пальцах (в координатах контента)

                                                    val dx = centroid.x * (1f / scale - 1f / oldScale)

                                                    val dy = centroid.y * (1f / scale - 1f / oldScale)



                                                    val nextX = offsetX + dx

                                                    offsetX = if (lockVerticalScroll) nextX.coerceIn(customMinX, customMaxX) else nextX

                                                    offsetY += dy

                                                }

                                                if (panChange != Offset.Zero) {

                                                    val newX = offsetX + (panChange.x / scale)


                                                    offsetX = if (lockVerticalScroll) newX.coerceIn(customMinX, customMaxX) else newX

                                                    if (!lockVerticalScroll) {

                                                        offsetY += panChange.y / scale

                                                    }

                                                }


// Track velocity

                                                event.changes.forEach {

                                                    velocityTracker.addPosition(it.uptimeMillis, it.position)

                                                    if (it.position != it.previousPosition) it.consume()

                                                }

                                            }

                                        }

                                    } while (!canceled && event.changes.any { it.pressed })



// Fling handling - ONLY if vertical scroll is locked and it was a single touch gesture

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

// Day of week label

                                val cal = remember(config.startMillis) {

                                    Calendar.getInstance().apply { timeInMillis = config.startMillis }

                                }

                                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...

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



// Day Label (e.g. 1. Пн)

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



// Date label at 06:00 (shifted from 02:00)

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



// Date label at 12:00 (reverted from 16:00)

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

                                    CurrentTimeLine(baseHourWidth, config.height)

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

            drawRect(

                color = backgroundColor,

                size = size

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

    timedLaneOffset: Int,

    onTaskClick: (Task) -> Unit

) {

    val endOfRange = dayStartMillis + (24 * 60 * 60 * 1000L)



    Box(modifier = Modifier.height(height).wrapContentHeight(Alignment.Top)) {

        taskPositions.forEach { position ->

            val task = position.task

            val taskStart = task.startTime ?: 0L

            val taskEnd = task.endTime ?: taskStart


            if (task.isAllDay || (taskStart < endOfRange && taskEnd > dayStartMillis)) {

// Determine the portion of the task visible in the current 00:00 - 24:00 window

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
                    Text(
                        text = task.title,
                        modifier = Modifier.padding(4.dp),
                        style = if (task.isAllDay) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                        maxLines = if (task.isAllDay) 1 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Travel time blocks
                if (!task.isAllDay) {
                    val travelColor = Color(task.color ?: MaterialTheme.colorScheme.primary.toArgb()).copy(alpha = 0.35f)
                    
                    if (task.travelTimeBeforeMs > 0) {
                        val beforeWidth = baseHourWidth * (task.travelTimeBeforeMs.toFloat() / 3600000f)
                        val beforeLeft = left - beforeWidth
                        val hours = task.travelTimeBeforeMs / 3600000
                        val mins = (task.travelTimeBeforeMs % 3600000) / 60000
                        val travelText = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"

                        Surface(
                            modifier = Modifier
                                .offset(x = beforeLeft, y = topOffset)
                                .width(beforeWidth)
                                .height(taskHeight),
                            color = travelColor,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = travelText, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = Color.White, 
                                    maxLines = 1,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                    if (task.travelTimeAfterMs > 0) {
                        val afterWidth = baseHourWidth * (task.travelTimeAfterMs.toFloat() / 3600000f)
                        val afterLeft = left + width
                        val hours = task.travelTimeAfterMs / 3600000
                        val mins = (task.travelTimeAfterMs % 3600000) / 60000
                        val travelText = if (hours > 0) "${hours}ч ${mins}м" else "${mins}м"

                        Surface(
                            modifier = Modifier
                                .offset(x = afterLeft, y = topOffset)
                                .width(afterWidth)
                                .height(taskHeight),
                            color = travelColor,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = travelText, 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = Color.White, 
                                    maxLines = 1,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentTimeLine(baseHourWidth: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)

    val timeOffset = baseHourWidth * (hour + minute / 60f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(2.dp)
            .offset(x = timeOffset)
            .background(Color.Red)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .background(Color.Red, shape = CircleShape)
        )
    }
}
