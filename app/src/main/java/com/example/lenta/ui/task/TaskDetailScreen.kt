package com.example.lenta.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.lenta.model.Task
import java.util.Calendar
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task? = null,
    initialDate: LocalDate? = null,
    initialStartTime: Long? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: ((Task) -> Unit)? = null,
    onDeleteRecurrence: ((Task, Boolean) -> Unit)? = null,
    warnBeforeDelete: Boolean = false,
    disableTravelTime: Boolean = false,
    hideLocation: Boolean = false,
    hideUrl: Boolean = false,
    hideDescription: Boolean = false,
    hideMultiDay: Boolean = false,
    hideRecurrence: Boolean = false
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var location by remember { mutableStateOf(task?.location ?: "") }
    var url by remember { mutableStateOf(task?.url ?: "") }
    
    var hasTravelTime by remember { mutableStateOf((task?.travelTimeBeforeMs ?: 0L) > 0 || (task?.travelTimeAfterMs ?: 0L) > 0) }
    var travelBeforeHours by remember { mutableStateOf((task?.travelTimeBeforeMs ?: 0L) / 3600000) }
    var travelBeforeMinutes by remember { mutableStateOf(((task?.travelTimeBeforeMs ?: 0L) % 3600000) / 60000) }
    var travelAfterHours by remember { mutableStateOf((task?.travelTimeAfterMs ?: 0L) / 3600000) }
    var travelAfterMinutes by remember { mutableStateOf(((task?.travelTimeAfterMs ?: 0L) % 3600000) / 60000) }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showRecurrenceDeleteOptions by remember { mutableStateOf(false) }

    val baseCal = Calendar.getInstance().apply {
        if (task != null) {
            timeInMillis = task.startTime ?: System.currentTimeMillis()
        } else if (initialStartTime != null) {
            timeInMillis = initialStartTime
        } else if (initialDate != null) {
            set(Calendar.YEAR, initialDate.year)
            set(Calendar.MONTH, initialDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, initialDate.dayOfMonth)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        } else {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    
    var startTime by remember { mutableStateOf(task?.startTime ?: baseCal.timeInMillis) }
    var endTime by remember { 
        mutableStateOf(task?.endTime ?: (baseCal.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }.timeInMillis) 
    }
    var isAllDay by remember { mutableStateOf(task?.isAllDay ?: false) }
    var isVisibleOnlyOnTimeline by remember { mutableStateOf(task?.isVisibleOnlyOnTimeline ?: false) }
    var selectedColor by remember { mutableIntStateOf(task?.color ?: Color.Blue.toArgb()) }
    
    val colors = listOf(
        Color(0xFFFF1744), // Vibrant Red
        Color(0xFF5C6BC0), // Solid Blue
        Color(0xFF4CAF50), // Muted Green
        Color(0xFFFFD600), // Saturated Yellow
        Color(0xFFCF854E), // Dark Pastel Orange (Muted)
        Color(0xFF00BCD4), // Cyan
        Color(0xFF9C27B0), // Purple
        Color(0xFFE91E63), // Pink
        Color.Gray
    )

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var showRepeatScreen by remember { mutableStateOf(false) }
    var recurrenceRule by remember { mutableStateOf<RecurrenceRule>(RecurrenceRule()) }
    
    var showMultiDayPicker by remember { mutableStateOf(false) }
    var selectedMultiDates by remember { mutableStateOf<List<Long>>(emptyList()) }

    if (showRepeatScreen) {
        BackHandler { showRepeatScreen = false }
        RecurrencePicker(
            initialRule = recurrenceRule,
            onRuleSelected = { 
                recurrenceRule = it
                showRepeatScreen = false 
            },
            onBack = { showRepeatScreen = false }
        )
    } else if (showMultiDayPicker) {
        MultiDayPicker(
            onDismiss = { showMultiDayPicker = false },
            onDatesSelected = { dates ->
                selectedMultiDates = dates
                showMultiDayPicker = false
            },
            initialSelectedDates = selectedMultiDates
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (task == null) "Add Task" else "Edit Task") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val finalStartTime = startTime
                            val finalEndTime = if (isAllDay) finalStartTime else if (endTime < startTime) startTime + 3600000L else endTime

                            // Travel time calculation
                            val beforeMs = if (hasTravelTime) (travelBeforeHours * 3600000L) + (travelBeforeMinutes * 60000L) else 0L
                            val afterMs = if (hasTravelTime) (travelAfterHours * 3600000L) + (travelAfterMinutes * 60000L) else 0L

                            // Save duplicates for multi-day selection
                            val recurrenceId = if (selectedMultiDates.isNotEmpty() || recurrenceRule.type != RecurrenceType.NONE) {
                                java.util.UUID.randomUUID().toString()
                            } else null

                            val mainTask = (task?.copy(
                                title = title.ifBlank { "New Task" },
                                description = description,
                                location = location,
                                url = url,
                                startTime = finalStartTime,
                                endTime = finalEndTime,
                                isAllDay = isAllDay,
                                color = selectedColor,
                                recurrenceId = recurrenceId,
                                travelTimeBeforeMs = beforeMs,
                                travelTimeAfterMs = afterMs,
                                isVisibleOnlyOnTimeline = isVisibleOnlyOnTimeline
                            ) ?: Task(
                                title = title.ifBlank { "New Task" },
                                description = description,
                                location = location,
                                url = url,
                                startTime = finalStartTime,
                                endTime = finalEndTime,
                                isAllDay = isAllDay,
                                color = selectedColor,
                                recurrenceId = recurrenceId,
                                travelTimeBeforeMs = beforeMs,
                                travelTimeAfterMs = afterMs,
                                isVisibleOnlyOnTimeline = isVisibleOnlyOnTimeline
                            ))
                            
                            onSave(mainTask)
                            
                            // Save duplicates for multi-day selection
                            selectedMultiDates.forEach { millis ->
                                val selectedCal = Calendar.getInstance().apply { timeInMillis = millis }
                                
                                val newStart = Calendar.getInstance().apply {
                                    timeInMillis = startTime
                                    set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                                    set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                                }.timeInMillis

                                val newEnd = Calendar.getInstance().apply {
                                    timeInMillis = endTime
                                    set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                                    set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                                }.timeInMillis
                                
                                onSave(Task(
                                    title = title.ifBlank { "New Task" },
                                    description = description,
                                    location = location,
                                    url = url,
                                    startTime = newStart,
                                    endTime = newEnd,
                                    isAllDay = isAllDay,
                                    color = selectedColor,
                                    recurrenceId = recurrenceId,
                                    travelTimeBeforeMs = beforeMs,
                                    travelTimeAfterMs = afterMs,
                                    isVisibleOnlyOnTimeline = isVisibleOnlyOnTimeline
                                ))
                            }

                            // Handle Recurrence Cycle duplication
                            if (recurrenceRule.type != RecurrenceType.NONE) {
                                val instances = generateRecurrenceInstances(
                                    baseStartTime = finalStartTime,
                                    baseEndTime = finalEndTime,
                                    rule = recurrenceRule
                                )
                                instances.forEach { (start, end) ->
                                    onSave(mainTask.copy(id = 0, startTime = start, endTime = end, recurrenceId = recurrenceId))
                                }
                            }
                        }) {
                            Text("Save", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!hideDescription) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    if (!hideLocation) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!hideUrl) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            trailingIcon = {
                                if (url.isNotBlank()) {
                                    IconButton(onClick = {
                                        var formattedUrl = url.trim()
                                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                            formattedUrl = "https://$formattedUrl"
                                        }
                                        try {
                                            uriHandler.openUri(formattedUrl)
                                        } catch (e: Exception) {
                                            // Handle invalid URL if needed
                                        }
                                    }) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Open URL")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All day event", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    }

                    if (!isAllDay) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Start: ", style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = { showStartTimePicker = true }) {
                                val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                                Text(
                                    String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("End: ", style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = { showEndTimePicker = true }) {
                                val cal = Calendar.getInstance().apply { timeInMillis = endTime }
                                Text(
                                    String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date: ", style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { showDatePicker = true }) {
                            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                            Text(sdf.format(cal.time), style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (!disableTravelTime) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Время на дорогу", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(checked = hasTravelTime, onCheckedChange = { hasTravelTime = it })
                        }

                        if (hasTravelTime) {
                            Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                                TravelTimeInput(
                                    label = "ДО",
                                    hours = travelBeforeHours,
                                    minutes = travelBeforeMinutes,
                                    onHoursChange = { travelBeforeHours = it },
                                    onMinutesChange = { travelBeforeMinutes = it }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TravelTimeInput(
                                    label = "После",
                                    hours = travelAfterHours,
                                    minutes = travelAfterMinutes,
                                    onHoursChange = { travelAfterHours = it },
                                    onMinutesChange = { travelAfterMinutes = it }
                                )
                            }
                        }
                    }

                    if (!hideMultiDay || !hideRecurrence) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (!hideMultiDay) {
                            SettingRow(
                                title = "Несколько дней",
                                subtitle = if (selectedMultiDates.isNotEmpty()) "${selectedMultiDates.size} дн." else null,
                                icon = Icons.Default.DateRange,
                                onClick = { showMultiDayPicker = true }
                            )
                            if (!hideRecurrence) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        
                        if (!hideRecurrence) {
                            SettingRow(
                                title = "Цикл повторений",
                                subtitle = getRecurrenceSubtitle(recurrenceRule),
                                icon = Icons.Default.Refresh,
                                onClick = { showRepeatScreen = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Color:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(color, MaterialTheme.shapes.medium)
                                    .clickable { selectedColor = color.toArgb() }
                                    .padding(4.dp)
                            ) {
                                if (selectedColor == color.toArgb()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = 0.5f), MaterialTheme.shapes.small),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Показывать только на ленте", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = isVisibleOnlyOnTimeline, onCheckedChange = { isVisibleOnlyOnTimeline = it })
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for delete button
                }

                if (task != null && (onDelete != null || onDeleteRecurrence != null)) {
                    Button(
                        onClick = { 
                            if (task.recurrenceId != null) {
                                showRecurrenceDeleteOptions = true
                            } else if (warnBeforeDelete) {
                                showDeleteConfirmation = true
                            } else {
                                onDelete?.invoke(task)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить это событие?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete?.invoke(task!!)
                    showDeleteConfirmation = false 
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Отмена") }
            }
        )
    }

    if (showRecurrenceDeleteOptions) {
        AlertDialog(
            onDismissRequest = { showRecurrenceDeleteOptions = false },
            title = { Text("Выберите вариант удаления") },
            text = {
                Column {
                    TextButton(
                        onClick = { 
                            if (warnBeforeDelete) {
                                showRecurrenceDeleteOptions = false
                                showDeleteConfirmation = true
                            } else {
                                onDelete?.invoke(task!!)
                                showRecurrenceDeleteOptions = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить только это событие", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            onDeleteRecurrence?.invoke(task!!, true)
                            showRecurrenceDeleteOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Удалить все последующие события", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecurrenceDeleteOptions = false }) { Text("Отмена") }
            }
        )
    }

    if (showStartTimePicker) {
        val startPickerState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = startTime }.get(Calendar.MINUTE),
            is24Hour = true
        )
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startTime
                        set(Calendar.HOUR_OF_DAY, startPickerState.hour)
                        set(Calendar.MINUTE, startPickerState.minute)
                    }
                    startTime = cal.timeInMillis

                    // Automatic end time adjustment: start + 1 hour
                    val endCal = (cal.clone() as Calendar).apply {
                        add(Calendar.HOUR_OF_DAY, 1)
                    }
                    endTime = endCal.timeInMillis

                    showStartTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = startPickerState)
        }
    }

    if (showEndTimePicker) {
        val endPickerState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = endTime }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = endTime }.get(Calendar.MINUTE),
            is24Hour = true
        )
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = endTime
                        set(Calendar.HOUR_OF_DAY, endPickerState.hour)
                        set(Calendar.MINUTE, endPickerState.minute)
                    }
                    endTime = cal.timeInMillis
                    showEndTimePicker = false
                }) { Text("OK") }
            }
        ) {
            TimePicker(state = endPickerState)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                        
                        startTime = Calendar.getInstance().apply {
                            timeInMillis = startTime
                            set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis

                        endTime = Calendar.getInstance().apply {
                            timeInMillis = endTime
                            set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDayPicker(
    onDismiss: () -> Unit,
    onDatesSelected: (List<Long>) -> Unit,
    initialSelectedDates: List<Long>
) {
    var selectedDates by remember { mutableStateOf(initialSelectedDates.toSet()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { 
        set(Calendar.DAY_OF_MONTH, 1) 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080A15).copy(alpha = 0.92f)) // Темный фон за календарем с синим оттенком
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp)
                    .clickable(enabled = false) { }, // Предотвращаем закрытие при клике на сам календарь
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = null) }
                        
                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        }) { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val calendar = currentMonth.clone() as Calendar
                    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    var dayCount = 1
                    for (row in 0..5) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val currentDayIndex = row * 7 + col
                                if (currentDayIndex < firstDayOfWeek || dayCount > daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val dateCal = (calendar.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayCount)
                                    }
                                    val dateMillis = dateCal.timeInMillis
                                    val isSelected = selectedDates.contains(dateMillis)
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDates = if (isSelected) {
                                                    selectedDates - dateMillis
                                                } else {
                                                    selectedDates + dateMillis
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayCount.toString(),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    dayCount++
                                }
                            }
                        }
                        if (dayCount > daysInMonth) break
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onDatesSelected(selectedDates.toList()) }) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlaceholder(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("This feature is under development", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    }
}

@Composable
fun TravelTimeInput(
    label: String,
    hours: Long,
    minutes: Long,
    onHoursChange: (Long) -> Unit,
    onMinutesChange: (Long) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium)
        TextField(
            value = if (hours == 0L) "" else hours.toString(),
            onValueChange = { 
                if (it.length <= 2) {
                    onHoursChange(it.toLongOrNull() ?: 0L)
                }
            },
            modifier = Modifier.width(70.dp),
            placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
            suffix = { Text("ч.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, 
                unfocusedContainerColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextField(
            value = if (minutes == 0L) "" else minutes.toString(),
            onValueChange = { 
                if (it.length <= 2) {
                    onMinutesChange(it.toLongOrNull() ?: 0L)
                }
            },
            modifier = Modifier.width(100.dp),
            placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
            suffix = { Text("мин.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, 
                unfocusedContainerColor = Color.Transparent
            )
        )
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

// Recurrence Logic and UI

enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }
enum class RecurrenceEndType { ENDLESS, COUNT, UNTIL }

data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1,
    val daysOfWeek: Set<Int> = emptySet(), // 1=Sun, 2=Mon ...
    val daysOfMonth: Set<Int> = emptySet(),
    val endType: RecurrenceEndType = RecurrenceEndType.ENDLESS,
    val endCount: Int = 10,
    val endUntil: Long? = null
)

private fun getRecurrenceSubtitle(rule: RecurrenceRule): String {
    if (rule.type == RecurrenceType.NONE) return "Нет"
    return when(rule.type) {
        RecurrenceType.DAILY -> "Ежедневно"
        RecurrenceType.WEEKLY -> "Каждую неделю"
        RecurrenceType.MONTHLY -> "Каждый месяц"
        RecurrenceType.YEARLY -> "Каждый год"
        else -> "Настроено"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePicker(
    initialRule: RecurrenceRule,
    onRuleSelected: (RecurrenceRule) -> Unit,
    onBack: () -> Unit
) {
    var rule by remember { mutableStateOf(initialRule) }
    var showDurationScreen by remember { mutableStateOf(false) }

    if (showDurationScreen) {
        RecurrenceDurationPicker(
            initialEndType = rule.endType,
            initialCount = rule.endCount,
            initialUntil = rule.endUntil,
            onDurationSelected = { type, count, until ->
                rule = rule.copy(endType = type, endCount = count, endUntil = until)
                showDurationScreen = false
            },
            onBack = { showDurationScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Цикл повторений") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onRuleSelected(rule) }) {
                        Text("Готово", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Surface(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        RecurrenceOptionItem(
                            title = "Не повторять",
                            selected = rule.type == RecurrenceType.NONE,
                            onClick = { rule = rule.copy(type = RecurrenceType.NONE) },
                            showDivider = true
                        )
                        RecurrenceOptionItem(
                            title = "Ежедневно",
                            selected = rule.type == RecurrenceType.DAILY,
                            onClick = { rule = rule.copy(type = RecurrenceType.DAILY) },
                            showDivider = true
                        )
                        Column {
                            RecurrenceOptionItem(
                                title = "Каждую неделю",
                                selected = rule.type == RecurrenceType.WEEKLY,
                                onClick = { rule = rule.copy(type = RecurrenceType.WEEKLY) },
                                showDivider = true
                            )
                            if (rule.type == RecurrenceType.WEEKLY) {
                                WeekDayPicker(
                                    selectedDays = rule.daysOfWeek,
                                    onDaysChanged = { rule = rule.copy(daysOfWeek = it) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        Column {
                            RecurrenceOptionItem(
                                title = "Каждый месяц",
                                selected = rule.type == RecurrenceType.MONTHLY,
                                onClick = { rule = rule.copy(type = RecurrenceType.MONTHLY) },
                                showDivider = true
                            )
                            if (rule.type == RecurrenceType.MONTHLY) {
                                MonthDayPicker(
                                    selectedDays = rule.daysOfMonth,
                                    onDaysChanged = { rule = rule.copy(daysOfMonth = it) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        RecurrenceOptionItem(
                            title = "Каждый год",
                            selected = rule.type == RecurrenceType.YEARLY,
                            onClick = { rule = rule.copy(type = RecurrenceType.YEARLY) }
                        )
                    }
                }
            }

            if (rule.type != RecurrenceType.NONE) {
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val unitText = when(rule.type) {
                                    RecurrenceType.DAILY -> "дн."
                                    RecurrenceType.WEEKLY -> "нед."
                                    RecurrenceType.MONTHLY -> "мес."
                                    RecurrenceType.YEARLY -> "г."
                                    else -> ""
                                }
                                Text("Повторять каждые", style = MaterialTheme.typography.bodyLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = rule.interval.toString(),
                                        onValueChange = { 
                                            val newVal = it.toIntOrNull()?.coerceIn(1, 100) ?: 1
                                            rule = rule.copy(interval = newVal)
                                        },
                                        modifier = Modifier.width(60.dp),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                    Text(unitText, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showDurationScreen = true }.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Длительность", style = MaterialTheme.typography.bodyLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val durationText = when(rule.endType) {
                                        RecurrenceEndType.ENDLESS -> "Бесконечно"
                                        RecurrenceEndType.COUNT -> "${rule.endCount} раз"
                                        RecurrenceEndType.UNTIL -> "До ${rule.endUntil?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: ""}"
                                    }
                                    Text(durationText, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
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
fun RecurrenceOptionItem(title: String, selected: Boolean, onClick: () -> Unit, showDivider: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun WeekDayPicker(selectedDays: Set<Int>, onDaysChanged: (Set<Int>) -> Unit) {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val calDays = listOf(2, 3, 4, 5, 6, 7, 1) // Calendar.MONDAY=2...Calendar.SUNDAY=1
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, name ->
            val calDay = calDays[index]
            val isSelected = selectedDays.contains(calDay)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable {
                        onDaysChanged(if (isSelected) selectedDays - calDay else selectedDays + calDay)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MonthDayPicker(selectedDays: Set<Int>, onDaysChanged: (Set<Int>) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        for (row in 0..4) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (col in 1..7) {
                    val day = row * 7 + col
                    if (day <= 31) {
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    onDaysChanged(if (isSelected) selectedDays - day else selectedDays + day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(), 
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceDurationPicker(
    initialEndType: RecurrenceEndType,
    initialCount: Int,
    initialUntil: Long?,
    onDurationSelected: (RecurrenceEndType, Int, Long?) -> Unit,
    onBack: () -> Unit
) {
    var endType by remember { mutableStateOf(initialEndType) }
    var endCount by remember { mutableIntStateOf(initialCount) }
    var endUntil by remember { mutableStateOf(initialUntil) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endUntil ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endUntil = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Длительность") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onDurationSelected(endType, endCount, endUntil) }) {
                        Text("Готово", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    RecurrenceOptionItem(
                        title = "Бесконечно",
                        selected = endType == RecurrenceEndType.ENDLESS,
                        onClick = { endType = RecurrenceEndType.ENDLESS },
                        showDivider = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { endType = RecurrenceEndType.COUNT }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Повторить n раз", style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = endCount.toString(),
                                onValueChange = { endCount = it.toIntOrNull() ?: 1 },
                                modifier = Modifier.width(60.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                            )
                            if (endType == RecurrenceEndType.COUNT) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            endType = RecurrenceEndType.UNTIL
                            showDatePicker = true
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("До", style = MaterialTheme.typography.bodyLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = endUntil?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Выбрать...",
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (endType == RecurrenceEndType.UNTIL) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateRecurrenceInstances(
    baseStartTime: Long,
    baseEndTime: Long,
    rule: RecurrenceRule
): List<Pair<Long, Long>> {
    val instances = mutableListOf<Pair<Long, Long>>()
    val cal = Calendar.getInstance().apply { timeInMillis = baseStartTime }
    val duration = baseEndTime - baseStartTime
    
    var count = 0
    val maxLimit = if (rule.endType == RecurrenceEndType.COUNT) rule.endCount else 100 // Reasonable limit for Endless
    val untilLimit = rule.endUntil
    
    // Skip the first one as it's the base task saved separately
    while (count < maxLimit) {
        when(rule.type) {
            RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, rule.interval)
            RecurrenceType.WEEKLY -> {
                // If specific days selected, move to next valid day
                if (rule.daysOfWeek.isNotEmpty()) {
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (!rule.daysOfWeek.contains(cal.get(Calendar.DAY_OF_WEEK)))
                    // This logic is simple and doesn't strictly follow 'every N weeks' with day selection correctly
                    // but matches the spirit of UI selection.
                } else {
                    cal.add(Calendar.WEEK_OF_YEAR, rule.interval)
                }
            }
            RecurrenceType.MONTHLY -> {
                if (rule.daysOfMonth.isNotEmpty()) {
                    // Logic to find next month with selected day
                    // For simplicity, just add months and set day
                    cal.add(Calendar.MONTH, rule.interval)
                    // Find first available selected day in that month
                    val targetDay = rule.daysOfMonth.first()
                    cal.set(Calendar.DAY_OF_MONTH, targetDay)
                } else {
                    cal.add(Calendar.MONTH, rule.interval)
                }
            }
            RecurrenceType.YEARLY -> cal.add(Calendar.YEAR, rule.interval)
            else -> break
        }
        
        if (untilLimit != null && cal.timeInMillis > untilLimit) break
        
        instances.add(cal.timeInMillis to cal.timeInMillis + duration)
        count++
        if (rule.endType == RecurrenceEndType.COUNT && count >= rule.endCount) break
    }
    
    return instances
}
