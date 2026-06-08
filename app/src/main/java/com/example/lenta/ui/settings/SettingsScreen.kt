package com.example.lenta.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onBack: () -> Unit
) {
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
                SettingsSectionTitle("Appearance")
                SettingsToggleItem(
                    title = "Dark Theme",
                    icon = Icons.Default.DarkMode,
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
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
            }

            item {
                SettingsSectionTitle("App Settings")
                SettingsClickItem(
                    title = "Calendar Settings",
                    icon = Icons.Default.CalendarMonth,
                    onClick = { /* Future: Navigate to sub-settings */ }
                )
            }
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
