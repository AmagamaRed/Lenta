package com.example.lenta.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.lenta.model.Task
import java.util.Calendar
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task? = null,
    initialDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: ((Task) -> Unit)? = null
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var location by remember { mutableStateOf(task?.location ?: "") }
    var url by remember { mutableStateOf(task?.url ?: "") }
    
    val baseCal = Calendar.getInstance().apply {
        if (task != null) {
            timeInMillis = task.startTime ?: System.currentTimeMillis()
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
    var selectedColor by remember { mutableIntStateOf(task?.color ?: Color.Blue.toArgb()) }
    
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray)

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var showRepeatScreen by remember { mutableStateOf(false) }
    var showMultiDayScreen by remember { mutableStateOf(false) }

    if (showRepeatScreen) {
        BackHandler { showRepeatScreen = false }
        FullScreenPlaceholder(title = "Repeat Settings", onBack = { showRepeatScreen = false })
    } else if (showMultiDayScreen) {
        BackHandler { showMultiDayScreen = false }
        FullScreenPlaceholder(title = "Multiple Days Settings", onBack = { showMultiDayScreen = false })
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
                            val finalEndTime = if (isAllDay) {
                                // If all day, make sure it covers the whole day or at least doesn't conflict
                                finalStartTime
                            } else if (endTime < startTime) {
                                startTime + 3600000L
                            } else {
                                endTime
                            }

                            onSave(
                                (task?.copy(
                                    title = title.ifBlank { "New Task" },
                                    description = description,
                                    location = location,
                                    url = url,
                                    startTime = finalStartTime,
                                    endTime = finalEndTime,
                                    isAllDay = isAllDay,
                                    color = selectedColor
                                ) ?: Task(
                                    title = title.ifBlank { "New Task" },
                                    description = description,
                                    location = location,
                                    url = url,
                                    startTime = finalStartTime,
                                    endTime = finalEndTime,
                                    isAllDay = isAllDay,
                                    color = selectedColor
                                ))
                            )
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
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Location field
                    TextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // URL field
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
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

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Setting-like rows
                    SettingRow(
                        title = "Repeat",
                        icon = Icons.Default.Refresh,
                        onClick = { showRepeatScreen = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingRow(
                        title = "Multiple Days",
                        icon = Icons.Default.DateRange,
                        onClick = { showMultiDayScreen = true }
                    )

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
                    
                    Spacer(modifier = Modifier.height(100.dp)) // Padding for delete button
                }

                if (task != null && onDelete != null) {
                    Button(
                        onClick = { onDelete(task) },
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
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
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
