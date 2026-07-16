package com.example.lenta.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    warnBeforeDelete: Boolean,
    onWarnBeforeDeleteChange: (Boolean) -> Unit,
    disableTravelTime: Boolean,
    onDisableTravelTimeChange: (Boolean) -> Unit,
    hideLocation: Boolean,
    onHideLocationChange: (Boolean) -> Unit,
    hideUrl: Boolean,
    onHideUrlChange: (Boolean) -> Unit,
    hideDescription: Boolean,
    onHideDescriptionChange: (Boolean) -> Unit,
    hideMultiDay: Boolean,
    onHideMultiDayChange: (Boolean) -> Unit,
    hideRecurrence: Boolean,
    onHideRecurrenceChange: (Boolean) -> Unit,
    easyModeChatLayout: Boolean,
    onEasyModeChatLayoutChange: (Boolean) -> Unit,
    disableTimelineListToggle: Boolean,
    onDisableTimelineListToggleChange: (Boolean) -> Unit,
    activeSubScreen: String?,
    onSubScreenChange: (String?) -> Unit,
    onBack: () -> Unit
) {
    val backgroundColor = Color(0xFF121212)

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
        "event_settings" -> {
            EventSettingsScreen(
                warnBeforeDelete = warnBeforeDelete,
                onWarnBeforeDeleteChange = onWarnBeforeDeleteChange,
                disableTravelTime = disableTravelTime,
                onDisableTravelTimeChange = onDisableTravelTimeChange,
                hideLocation = hideLocation,
                onHideLocationChange = onHideLocationChange,
                hideUrl = hideUrl,
                onHideUrlChange = onHideUrlChange,
                hideDescription = hideDescription,
                onHideDescriptionChange = onHideDescriptionChange,
                hideMultiDay = hideMultiDay,
                onHideMultiDayChange = onHideMultiDayChange,
                hideRecurrence = hideRecurrence,
                onHideRecurrenceChange = onHideRecurrenceChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
        "easy_mode_settings" -> {
            EasyModeSettingsScreen(
                chatLayout = easyModeChatLayout,
                onChatLayoutChange = onEasyModeChatLayoutChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
        "timeline_view" -> {
            TimelineViewSettingsScreen(
                scale = timelineScale,
                onScaleChange = onTimelineScaleChange,
                stack = timelineStackTasks,
                onStackChange = onTimelineStackTasksChange,
                disableToggle = disableTimelineListToggle,
                onDisableToggleChange = onDisableTimelineListToggleChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
        "timeline_limits" -> {
            TimelineLimitsSettingsScreen(
                lock = lockVerticalScroll,
                onLockChange = onLockVerticalScrollChange,
                minX = timelineMinX,
                onMinXChange = onTimelineMinXChange,
                maxX = timelineMaxX,
                onMaxXChange = onTimelineMaxXChange,
                onBack = { onSubScreenChange(null) }
            )
            return
        }
        "language" -> {
            LanguageSettingsScreen(onBack = { onSubScreenChange(null) })
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionTitle("ОФОРМЛЕНИЕ")
                SettingsGroup {
                    SettingsItem(
                        title = "Цвет темы",
                        icon = Icons.Default.Palette,
                        onClick = { onSubScreenChange("appearance") }
                    )
                }
            }
            
            item {
                SettingsSectionTitle("ТАЙМЛАЙН")
                SettingsGroup {
                    SettingsItem(
                        title = "Вид ленты",
                        icon = Icons.Default.LinearScale,
                        onClick = { onSubScreenChange("timeline_view") }
                    )
                    SettingsItem(
                        title = "Границы и блокировка",
                        icon = Icons.Default.Lock,
                        onClick = { onSubScreenChange("timeline_limits") }
                    )
                }
            }

            item {
                SettingsSectionTitle("Easy mode")
                SettingsGroup {
                    SettingsItem(
                        title = "Настройки заметок",
                        icon = Icons.Default.Description,
                        onClick = { onSubScreenChange("easy_mode_settings") }
                    )
                }
            }

            item {
                SettingsSectionTitle("ПРИЛОЖЕНИЕ")
                SettingsGroup {
                    SettingsItem(
                        title = "Настройки календаря",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onSubScreenChange("calendar") },
                        showDivider = true
                    )
                    SettingsItem(
                        title = "Настройки событий",
                        icon = Icons.Default.Event,
                        onClick = { onSubScreenChange("event_settings") },
                        showDivider = true
                    )
                    SettingsItem(
                        title = "Язык",
                        icon = Icons.Default.Language,
                        onClick = { onSubScreenChange("language") }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title, 
                modifier = Modifier.weight(1f), 
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineViewSettingsScreen(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    stack: Boolean,
    onStackChange: (Boolean) -> Unit,
    disableToggle: Boolean,
    onDisableToggleChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вид ленты") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsSectionTitle("ВЫСОТА")
                SettingsGroup {
                    SettingsSliderItem(
                        title = "Высота дорожки",
                        icon = Icons.Default.Tune,
                        value = scale,
                        onValueChange = onScaleChange,
                        valueRange = 0.1f..2.0f
                    )
                }
            }
            item {
                SettingsSectionTitle("ОТОБРАЖЕНИЕ")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Совмещенные ленты",
                        icon = Icons.Default.Palette,
                        checked = stack,
                        onCheckedChange = onStackChange
                    )
                    SettingsToggleItem(
                        title = "Отключить переход в список",
                        icon = Icons.Default.ListAlt,
                        checked = disableToggle,
                        onCheckedChange = onDisableToggleChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineLimitsSettingsScreen(
    lock: Boolean,
    onLockChange: (Boolean) -> Unit,
    minX: Float,
    onMinXChange: (Float) -> Unit,
    maxX: Float,
    onMaxXChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Границы и блокировка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsSectionTitle("БЛОКИРОВКА")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Lock Vertical Scroll",
                        icon = Icons.Default.Lock,
                        checked = lock,
                        onCheckedChange = onLockChange
                    )
                }
            }
            item {
                SettingsSectionTitle("РУЧНЫЕ ГРАНИЦЫ")
                SettingsGroup {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxX.toString(),
                                onValueChange = { onMaxXChange(it.toFloatOrNull() ?: 0f) },
                                label = { Text("Max X (Left)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = minX.toString(),
                                onValueChange = { onMinXChange(it.toFloatOrNull() ?: 0f) },
                                label = { Text("Min X (Right)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettingsScreen(
    warnBeforeDelete: Boolean,
    onWarnBeforeDeleteChange: (Boolean) -> Unit,
    disableTravelTime: Boolean,
    onDisableTravelTimeChange: (Boolean) -> Unit,
    hideLocation: Boolean,
    onHideLocationChange: (Boolean) -> Unit,
    hideUrl: Boolean,
    onHideUrlChange: (Boolean) -> Unit,
    hideDescription: Boolean,
    onHideDescriptionChange: (Boolean) -> Unit,
    hideMultiDay: Boolean,
    onHideMultiDayChange: (Boolean) -> Unit,
    hideRecurrence: Boolean,
    onHideRecurrenceChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки событий") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsSectionTitle("ДОБАВЛЕНИЕ И РЕДАКТИРОВАНИЕ")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Убрать Location",
                        icon = Icons.Default.LocationOn,
                        checked = hideLocation,
                        onCheckedChange = onHideLocationChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        title = "Убрать URL",
                        icon = Icons.Default.Link,
                        checked = hideUrl,
                        onCheckedChange = onHideUrlChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        title = "Убрать Описание",
                        icon = Icons.Default.Description,
                        checked = hideDescription,
                        onCheckedChange = onHideDescriptionChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        title = "Убрать \"Несколько дней\"",
                        icon = Icons.Default.DateRange,
                        checked = hideMultiDay,
                        onCheckedChange = onHideMultiDayChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        title = "Убрать \"Цикл повторений\"",
                        icon = Icons.Default.Refresh,
                        checked = hideRecurrence,
                        onCheckedChange = onHideRecurrenceChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        title = "Отключить \"Время на дорогу\"",
                        icon = Icons.Default.TimerOff,
                        checked = disableTravelTime,
                        onCheckedChange = onDisableTravelTimeChange
                    )
                }
            }

            item {
                SettingsSectionTitle("УДАЛЕНИЕ")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Предупреждать перед удалением",
                        icon = Icons.Default.Warning,
                        checked = warnBeforeDelete,
                        onCheckedChange = onWarnBeforeDeleteChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyModeSettingsScreen(
    chatLayout: Boolean,
    onChatLayoutChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки заметок") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SettingsSectionTitle("ДОБАВЛЕНИЕ ЗАМЕТОК")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Режим Чата",
                        icon = Icons.Default.Tune,
                        checked = chatLayout,
                        onCheckedChange = onChatLayoutChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Язык") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Скоро...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
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
                title = { Text("Цвет темы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionTitle("ТЕМА")
                SettingsGroup {
                    SettingsToggleItem(
                        title = "Темная тема",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkTheme,
                        onCheckedChange = onThemeChange
                    )
                }
            }

            item {
                SettingsSectionTitle("БАР ДНЕЙ (КАЛЕНДАРЬ)")
                SettingsGroup {
                    SettingsClickItemPlain(
                        title = "Цвет бара",
                        icon = Icons.Default.Palette,
                        onClick = { showColorPickerFor = "dayBar" },
                        showDivider = true
                    )
                    SettingsOpacityItemPlain(
                        title = "Прозрачность бара",
                        value = dayBarOpacity,
                        onValueChange = onDayBarOpacityChange
                    )
                }
            }

            item {
                SettingsSectionTitle("СЕТКА (КАЛЕНДАРЬ)")
                SettingsGroup {
                    SettingsClickItemPlain(
                        title = "Цвет линий сетки",
                        icon = Icons.Default.Palette,
                        onClick = { showColorPickerFor = "grid" },
                        showDivider = true
                    )
                    SettingsOpacityItemPlain(
                        title = "Прозрачность сетки",
                        value = gridOpacity,
                        onValueChange = onGridOpacityChange
                    )
                }
            }

            item {
                SettingsSectionTitle("СПИСОК ЗАДАЧ(calendar)")
                SettingsGroup {
                    SettingsClickItemPlain(
                        title = "Цвет фона",
                        icon = Icons.Default.Palette,
                        onClick = { showColorPickerFor = "taskList" },
                        showDivider = true
                    )
                    SettingsOpacityItemPlain(
                        title = "Яркость",
                        value = taskListBrightness,
                        onValueChange = onTaskListBrightnessChange
                    )
                }
            }

            item {
                SettingsSectionTitle("МАРКЕРЫ (calendar)")
                SettingsGroup {
                    SettingsClickItemPlain(
                        title = "Цвет текущего дня",
                        icon = Icons.Default.Palette,
                        onClick = { showColorPickerFor = "today" }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettingsOpacityItemPlain(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f
        )
    }
}

@Composable
fun SettingsClickItemPlain(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
        }
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
        title = { Text("Выберите цвет") },
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
            TextButton(onClick = onDismiss) { Text("Отмена") }
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
            title = { Text("Выберите настройку") },
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
                        title = "Переключение между календарями",
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
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки календаря") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSectionTitle("Предпочитаемый вид")
                SettingsGroup {
                    SettingsClickItemPlain(
                        title = "Настройка переключения календарей",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { showDialog = true }
                    )
                }
                
                val prefText = when(currentPreference) {
                    2 -> "Заблокировать смену календарей"
                    else -> "Переключение между календарями"
                }
                Text(
                    text = "Текущий: $prefText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
