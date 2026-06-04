package com.example.lenta.ui.easymode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lenta.model.Task

@Composable
fun EasyModeScreen(
    tasks: List<Task>,
    onAddTask: (String) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onDeleteCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    // Sort tasks: completed go to the bottom, then by creation date
    val sortedTasks = remember(tasks) {
        tasks.filter { it.isEasyModeEntry }.sortedWith(
            compareBy<Task> { it.isCompleted }.thenByDescending { it.createdAt }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Easy Mode",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = { showDeleteCompletedDialog = true }) {
                Text("🗑️")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onToggle = { onUpdateTask(task.copy(isCompleted = !task.isCompleted)) },
                    onClick = { taskToDelete = task }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Quick add task...") }
            )
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAddTask(text)
                        text = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Add")
            }
        }
    }

    if (showDeleteCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedDialog = false },
            title = { Text("Delete Completed") },
            text = { Text("Are you sure you want to delete all completed tasks?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCompleted()
                    showDeleteCompletedDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Delete '${taskToDelete?.title}'?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteTask(taskToDelete!!)
                    taskToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val alpha = if (task.isCompleted) 0.5f else 1f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                )
                
                // Full-width strike-through for the whole area if completed
                if (task.isCompleted) {
                    HorizontalDivider(
                        modifier = Modifier.align(Alignment.Center),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
