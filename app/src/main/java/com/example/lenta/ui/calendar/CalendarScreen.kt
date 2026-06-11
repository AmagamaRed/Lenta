package com.example.lenta.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import com.example.lenta.model.Task
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
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
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddTaskClick: (LocalDate) -> Unit,
    viewMode: CalendarViewMode = CalendarViewMode.MONTHLY,
    onViewModeChange: (CalendarViewMode) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    dayBarColor: Int = -1,
    dayBarOpacity: Float = 1f,
    gridColor: Int = -3355444,
    gridOpacity: Float = 0.5f,
    taskListColor: Int = -1,
    taskListBrightness: Float = 1f,
    todayColor: Int = -16738680,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val coroutineScope = rememberCoroutineScope()

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek,
        outDateStyle = OutDateStyle.EndOfGrid
    )

    // Continuous dates for vertical view
    val allDates = remember(startMonth, endMonth) {
        val list = mutableListOf<LocalDate>()
        val firstDay = startMonth.atDay(1)
        val padding = (firstDay.dayOfWeek.value % 7) - (firstDayOfWeek.value % 7)
        val normalizedPadding = if (padding < 0) padding + 7 else padding
        
        repeat(normalizedPadding) { list.add(LocalDate.MIN) }
        
        var curr = firstDay
        val end = endMonth.atEndOfMonth()
        while (!curr.isAfter(end)) {
            list.add(curr)
            curr = curr.plusDays(1)
        }
        list
    }

    val gridState = rememberLazyGridState()
    val textMeasurer = rememberTextMeasurer()
    val monthLabelStyle = MaterialTheme.typography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        fontSize = 74.sp
    )
    
    var isInitialized by remember { mutableStateOf(false) }

    // Sync vertical scroll to current month on start
    LaunchedEffect(isInitialized, viewMode) {
        if (!isInitialized && viewMode == CalendarViewMode.VERTICAL_LIST) {
            val today = LocalDate.now()
            val index = allDates.indexOf(today.withDayOfMonth(1))
            if (index != -1) {
                gridState.scrollToItem(maxOf(0, index - 7))
            }
            isInitialized = true
        }
    }

    // Mapping for month center indicators
    val monthCenterDates = remember(allDates) {
        val centers = mutableMapOf<LocalDate, Int>()
        var curr = startMonth
        while (!curr.isAfter(endMonth)) {
            val first = curr.atDay(1)
            val index = allDates.indexOf(first)
            if (index != -1) {
                val row = index / 7
                val targetRow = row + 2
                val thursdayIndex = (targetRow * 7) + 3 // Index 3 is Thursday (Mon=0, Tue=1, Wed=2, Thu=3)
                if (thursdayIndex < allDates.size) {
                    centers[allDates[thursdayIndex]] = curr.monthValue
                }
            }
            curr = curr.plusMonths(1)
        }
        centers
    }

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
            onDateSelected(null)
        }
    }

    val finalGridColor = Color(gridColor).copy(alpha = gridOpacity)
    val finalDayBarColor = if (dayBarColor == -1) MaterialTheme.colorScheme.surfaceVariant else Color(dayBarColor).copy(alpha = dayBarOpacity)
    
    // Apply brightness to task list color
    val baseTaskListColor = if (taskListColor == -1) MaterialTheme.colorScheme.surface else Color(taskListColor)
    val finalTaskListColor = baseTaskListColor.copy(
        red = (baseTaskListColor.red * taskListBrightness).coerceIn(0f, 1f),
        green = (baseTaskListColor.green * taskListBrightness).coerceIn(0f, 1f),
        blue = (baseTaskListColor.blue * taskListBrightness).coerceIn(0f, 1f),
        alpha = 1.0f // Always 100% opaque
    )
    val finalTodayColor = Color(todayColor)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val visibleMonth = if (viewMode == CalendarViewMode.MONTHLY) {
                remember(state.firstVisibleMonth) { state.firstVisibleMonth.yearMonth }
            } else {
                remember(gridState.firstVisibleItemIndex) {
                    val firstVisible = allDates.getOrNull(gridState.firstVisibleItemIndex)
                    if (firstVisible != null && firstVisible != LocalDate.MIN) YearMonth.from(firstVisible)
                    else YearMonth.now()
                }
            }
            Text(
                text = visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = { 
                    coroutineScope.launch {
                        val today = LocalDate.now()
                        if (viewMode == CalendarViewMode.MONTHLY) {
                            state.scrollToMonth(YearMonth.now())
                        } else {
                            val index = allDates.indexOf(today)
                            if (index != -1) {
                                // Direct scroll first to avoid long erratic animations if far away
                                gridState.scrollToItem(maxOf(0, index - 14))
                                gridState.animateScrollToItem(maxOf(0, index - 7))
                            }
                        }
                        onDateSelected(today)
                    }
                }) {
                    Text("🎯") // Today icon
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Day of week bar (Static)
                val daysOfWeek = remember { 
                    val firstDay = firstDayOfWeekFromLocale()
                    (0..6).map { firstDay.plus(it.toLong()) }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(finalDayBarColor)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (dayOfWeek in daysOfWeek) {
                        val isSunday = dayOfWeek == java.time.DayOfWeek.SUNDAY
                        Text(
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            text = dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSunday) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(finalGridColor)
                ) {
                    if (viewMode == CalendarViewMode.MONTHLY) {
                        HorizontalCalendar(
                            state = state,
                            dayContent = { day ->
                                if (day.position == DayPosition.MonthDate) {
                                    val dayTasks = tasks.filter { task ->
                                        task.startTime?.let {
                                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == day.date
                                        } ?: false
                                    }
                                    Day(
                                        day = day,
                                        isSelected = selectedDate == day.date,
                                        dayTasks = dayTasks,
                                        gridColor = finalGridColor,
                                        todayColor = finalTodayColor,
                                        onClick = { clickedDay -> onDateSelected(clickedDay.date) }
                                    )
                                }
                            },
                            monthHeader = { }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface) // Base background for the whole grid
                                .clipToBounds()
                                .drawBehind {
                                    val labelColor = primaryColor.copy(alpha = 0.18f)
                                    val visibleItems = gridState.layoutInfo.visibleItemsInfo
                                    visibleItems.forEach { item ->
                                        val date = allDates.getOrNull(item.index)
                                        val month = monthCenterDates[date]
                                        if (month != null) {
                                            val textLayoutResult = textMeasurer.measure(
                                                text = month.toString(),
                                                style = monthLabelStyle
                                            )
                                            // Center the text in the cell
                                            val x = item.offset.x + (item.size.width - textLayoutResult.size.width) / 2f
                                            val y = item.offset.y + (item.size.height - textLayoutResult.size.height) / 2f
                                            
                                            drawText(
                                                textLayoutResult = textLayoutResult,
                                                color = labelColor,
                                                topLeft = Offset(x.toFloat(), y.toFloat())
                                            )
                                        }
                                    }
                                }
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(7),
                                state = gridState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(allDates) { index, date ->
                                    if (date == LocalDate.MIN) {
                                        Box(modifier = Modifier.aspectRatio(0.5f).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)))
                                    } else {
                                        val dayTasks = tasks.filter { task ->
                                            task.startTime?.let {
                                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() == date
                                            } ?: false
                                        }
                                        VerticalDayCell(
                                            date = date,
                                            dayTasks = dayTasks,
                                            isSelected = selectedDate == date,
                                            gridColor = finalGridColor,
                                            todayColor = finalTodayColor,
                                            monthIndicator = null, // Logic moved to parent drawBehind
                                            onClick = { onDateSelected(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedDate != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                        .align(Alignment.BottomCenter),
                    color = finalTaskListColor,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row {
                                Button(
                                    onClick = { onAddTaskClick(selectedDate) },
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Add Task", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onDateSelected(null) }) {
                                    Text("✕")
                                }
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
fun VerticalDayCell(
    date: LocalDate,
    dayTasks: List<Task>,
    isSelected: Boolean,
    gridColor: Color,
    todayColor: Color,
    monthIndicator: Int?,
    onClick: (LocalDate) -> Unit
) {
    val isToday = remember(date) { date == LocalDate.now() }
    val isMonthStart = date.dayOfMonth == 1
    val isFirstWeek = date.dayOfMonth <= 7
    val baseGridColor = gridColor.copy(alpha = 1f) // Full opacity for bold lines

    Box(
        modifier = Modifier
            .aspectRatio(0.5f)
            .drawBehind {
                // Base grid lines
                drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 0.5.dp.toPx())
                drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 0.5.dp.toPx())
                drawLine(gridColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 0.5.dp.toPx())
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 0.5.dp.toPx())

                // Bold Month Separators
                if (isFirstWeek) {
                    drawLine(
                        color = baseGridColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                // Bold left border only if it's the 1st day AND NOT a Monday
                if (isMonthStart && date.dayOfWeek != java.time.DayOfWeek.MONDAY) {
                    drawLine(
                        color = baseGridColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .background(
                color = if (isSelected) Color.Transparent 
                        else if (isToday) todayColor.copy(alpha = 0.2f)
                        else Color.Transparent
            )
            .then(
                if (isSelected) Modifier.border(1.5.dp, todayColor)
                else if (isToday) Modifier.border(1.5.dp, todayColor)
                else Modifier
            )
            .clickable { onClick(date) },
        contentAlignment = Alignment.TopCenter
    ) {
        if (monthIndicator != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = monthIndicator.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.graphicsLayer(scaleX = 1.4f, scaleY = 1.4f)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected || isToday) todayColor else Color.Unspecified,
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
fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    dayTasks: List<Task>,
    gridColor: Color,
    todayColor: Color,
    onClick: (CalendarDay) -> Unit
) {
    val isToday = remember(day.date) { day.date == LocalDate.now() }
    val dayOfWeek = day.date.dayOfWeek

    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .padding(
                start = if (dayOfWeek == java.time.DayOfWeek.MONDAY) 3.dp else 0.dp,
                end = if (dayOfWeek == java.time.DayOfWeek.SUNDAY) 3.dp else 0.dp
            )
            .border(0.5.dp, gridColor)
            .background(
                color = if (isSelected) Color.Transparent
                        else if (isToday) todayColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
            )
            .then(
                if (isSelected) Modifier.border(1.5.dp, todayColor)
                else if (isToday) Modifier.border(1.5.dp, todayColor)
                else Modifier
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick(day) },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) {
                    if (isSelected || isToday) todayColor else Color.Unspecified
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
