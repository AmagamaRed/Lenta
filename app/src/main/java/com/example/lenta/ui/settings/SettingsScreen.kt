package com.example.lenta.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    timelineScale: Float,
    onTimelineScaleChange: (Float) -> Unit,
    timelineStackTasks: Boolean,
    onTimelineStackTasksChange: (Boolean) -> Unit,
    lockVerticalScroll: Boolean,
    onLockVerticalScrollChange: (Boolean) -> Unit,
    timelineMinX: Float,
    onTimelineMinXChange: (Float) -> Unit,
    timelineMaxX: Float,
    onTimelineMaxXChange: (Float) -> Unit,
    calendarPreference: Int,
    onCalendarPreferenceChange: (Int) -> Unit,
    dayBarColor: Int,
    onDayBarColorChange: (Int) -> Unit,
    dayBarOpacity: Float,
    onDayBarOpacityChange: (Float) -> Unit,
    gridColor: Int,
    onGridColorChange: (Int) -> Unit,
    gridOpacity: Float,
    onGridOpacityChange: (Float) -> Unit,
    taskListColor: Int,
    onTaskListColorChange: (Int) -> Unit,
    taskListBrightness: Float,
    onTaskListBrightnessChange: (Float) -> Unit,
    todayColor: Int,
    onTodayColorChange: (Int) -> Unit,
    activeSubScreen: String?,
    onSubScreenChange: (String?) -> Unit,
    onBack: () -> Unit
) {
    when (activeSubScreen) {
        "calendar" -> {
            CalendarSettingsScreen(
                currentPreference = calendarPreference,
                onPreferenceChange = onCalendarPreferenceChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
        "appearance" -> {
            AppearanceSettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                dayBarColor = dayBarColor,
                onDayBarColorChange = onDayBarColorChange,
                dayBarOpacity = dayBarOpacity,
                onDayBarOpacityChange = onDayBarOpacityChange,
                gridColor = gridColor,
                onGridColorChange = onGridColorChange,
                gridOpacity = gridOpacity,
                onGridOpacityChange = onGridOpacityChange,
                taskListColor = taskListColor,
                onTaskListColorChange = onTaskListColorChange,
                taskListBrightness = taskListBrightness,
                onTaskListBrightnessChange = onTaskListBrightnessChange,
                todayColor = todayColor,
                onTodayColorChange = onTodayColorChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionTitle("Personalization")
                SettingsClickItem(
                    title = "Appearance",
                    icon = Icons.Default.Palette,
                    onClick = { onSubScreenChange("appearance") }
                )
            }
            
            item {
                SettingsSectionTitle("Timeline")
                SettingsSliderItem(
                    title = "Lane Height",
                    icon = Icons.Default.LinearScale,
                    value = timelineScale,
                    onValueChange = onTimelineScaleChange,
                    valueRange = 0.1f..2.0f
                )
                SettingsToggleItem(
                    title = "Stick Timelines",
                    icon = Icons.Default.Palette,
                    checked = timelineStackTasks,
                    onCheckedChange = onTimelineStackTasksChange
                )
                SettingsToggleItem(
                    title = "Lock Vertical Scroll",
                    icon = Icons.Default.Lock,
                    checked = lockVerticalScroll,
                    onCheckedChange = onLockVerticalScrollChange
                )
                
                // Manual boundaries inputs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = timelineMaxX.toString(),
                        onValueChange = { onTimelineMaxXChange(it.toFloatOrNull() ?: 0f) },
                        label = { Text("Max X (Left)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timelineMinX.toString(),
                        onValueChange = { onTimelineMinXChange(it.toFloatOrNull() ?: 0f) },
                        label = { Text("Min X (Right)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                SettingsSectionTitle("App Settings")
                SettingsClickItem(
                    title = "Calendar Settings",
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onSubScreenChange("calendar") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    dayBarColor: Int,
    onDayBarColorChange: (Int) -> Unit,
    dayBarOpacity: Float,
    onDayBarOpacityChange: (Float) -> Unit,
    gridColor: Int,
    onGridColorChange: (Int) -> Unit,
    gridOpacity: Float,
    onGridOpacityChange: (Float) -> Unit,
    taskListColor: Int,
    onTaskListColorChange: (Int) -> Unit,
    taskListBrightness: Float,
    onTaskListBrightnessChange: (Float) -> Unit,
    todayColor: Int,
    onTodayColorChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showColorPickerFor by remember { mutableStateOf<String?>(null) }

    if (showColorPickerFor != null) {
        val initialCol = when(showColorPickerFor) {
            "dayBar" -> Color(dayBarColor)
            "grid" -> Color(gridColor)
            "taskList" -> if (taskListColor == -1) MaterialTheme.colorScheme.surface else Color(taskListColor)
            else -> Color(todayColor)
        }
        SimpleColorPickerDialog(
            initialColor = initialCol,
            onColorSelected = {
                when(showColorPickerFor) {
                    "dayBar" -> onDayBarColorChange(it.toArgb())
                    "grid" -> onGridColorChange(it.toArgb())
                    "taskList" -> onTaskListColorChange(it.toArgb())
                    "today" -> onTodayColorChange(it.toArgb())
                }
                showColorPickerFor = null
            },
            onDismiss = { showColorPickerFor = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionTitle("Theme")
                SettingsToggleItem(
                    title = "Dark Theme",
                    icon = Icons.Default.DarkMode,
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
                )
            }

            item {
                SettingsSectionTitle("Day Bar (Calendar)")
                SettingsClickItem(
                    title = "Day Bar Color",
                    icon = Icons.Default.Palette,
                    onClick = { showColorPickerFor = "dayBar" }
                )
                SettingsOpacityItem(
                    title = "Day Bar Opacity",
                    value = dayBarOpacity,
                    onValueChange = onDayBarOpacityChange
                )
            }

            item {
                SettingsSectionTitle("Grid (Calendar)")
                SettingsClickItem(
                    title = "Grid Line Color",
                    icon = Icons.Default.Palette,
                    onClick = { showColorPickerFor = "grid" }
                )
                SettingsOpacityItem(
                    title = "Grid Opacity",
                    value = gridOpacity,
                    onValueChange = onGridOpacityChange
                )
            }

            item {
                SettingsSectionTitle("Task List Overlay")
                SettingsClickItem(
                    title = "Background Color",
                    icon = Icons.Default.Palette,
                    onClick = { showColorPickerFor = "taskList" }
                )
                SettingsOpacityItem(
                    title = "Background Brightness",
                    value = taskListBrightness,
                    onValueChange = onTaskListBrightnessChange
                )
            }

            item {
                SettingsSectionTitle("Markers")
                SettingsClickItem(
                    title = "Today/Selection Color",
                    icon = Icons.Default.Palette,
                    onClick = { showColorPickerFor = "today" }
                )
            }
        }
    }
}

@Composable
fun SettingsOpacityItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
fun SimpleColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta,
        Color.Gray, Color.Black, Color.White, Color.DarkGray, Color.LightGray,
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFBB86FC), Color(0xFFFF5722),
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFE91E63)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                            .background(color, MaterialTheme.shapes.small)
                            .border(
                                if (color == initialColor) 2.dp else 0.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    currentPreference: Int,
    onPreferenceChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Calendar View") },
            text = {
                Column {
                    CalendarPreferenceItem(
                        title = "Заблокировать смену календарей",
                        selected = currentPreference == 2,
                        onClick = { 
                            onPreferenceChange(2)
                            showDialog = false
                        }
                    )
                    CalendarPreferenceItem(
                        title = "3. Оба календаря (Toggle)",
                        selected = currentPreference == 0,
                        onClick = { 
                            onPreferenceChange(0)
                            showDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SettingsSectionTitle("Configuration")
            
            SettingsClickItem(
                title = "Preferred Calendar View",
                icon = Icons.Default.CalendarMonth,
                onClick = { showDialog = true }
            )
            
            val prefText = when(currentPreference) {
                1 -> "Календарь В (Vertical)"
                2 -> "Заблокировать смену календарей"
                else -> "Оба календаря (Toggle)"
            }
            Text(
                text = "Current: $prefText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun CalendarPreferenceItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(String.format(java.util.Locale.getDefault(), "%.1fx", value), style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
