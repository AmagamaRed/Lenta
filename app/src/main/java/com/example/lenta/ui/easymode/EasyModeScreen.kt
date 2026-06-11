package com.example.lenta.ui.easymode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenta.model.Category
import com.example.lenta.model.Task

@Composable
fun EasyModeScreen(
    tasks: List<Task>,
    categories: List<Category>,
    activeChatId: Long,
    isChatMode: Boolean,
    onChatChange: (Long) -> Unit,
    onAddChat: (String, Int) -> Unit,
    onAddTask: (String, Boolean) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onDeleteCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showChatMenu by remember { mutableStateOf(false) }
    var showAddChatDialog by remember { mutableStateOf(false) }
    
    var isCheckboxMode by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    
    val activeChatName = remember(activeChatId, categories) {
        if (activeChatId == -1L) "Общий чат"
        else categories.find { it.id == activeChatId }?.name ?: "Чат"
    }

    val filteredTasks = remember(tasks, activeChatId) {
        tasks.filter { 
            it.isEasyModeEntry && 
            (if (activeChatId == -1L) it.categoryId == null else it.categoryId == activeChatId)
        }
    }

    val sortedTasks = remember(filteredTasks, isChatMode) {
        if (isChatMode) {
            filteredTasks.sortedByDescending { it.createdAt }
        } else {
            filteredTasks.sortedWith(
                compareBy<Task> { it.isCompleted }.thenByDescending { it.createdAt }
            )
        }
    }

    // Auto-scroll when new task is added
    LaunchedEffect(sortedTasks.size) {
        if (sortedTasks.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // We use a simple Column and let the system (adjustResize) handle the keyboard.
    // This avoids the "double padding" gap and ensures the header stays top.
    Column(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // Pinned Header
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier.clickable { showChatMenu = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeChatName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(expanded = showChatMenu, onDismissRequest = { showChatMenu = false }) {
                        DropdownMenuItem(text = { Text("Общий чат") }, onClick = { onChatChange(-1L); showChatMenu = false })
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).background(Color(category.color), RoundedCornerShape(2.dp)))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                },
                                onClick = { onChatChange(category.id); showChatMenu = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Добавить чат")
                                }
                            },
                            onClick = { showAddChatDialog = true; showChatMenu = false }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isCheckboxMode = !isCheckboxMode },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isCheckboxMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isCheckboxMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Toggle Checkbox Mode",
                            tint = if (isCheckboxMode) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { showDeleteCompletedDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Completed", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Chat List
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = isChatMode
            ) {
                items(sortedTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { onUpdateTask(task.copy(isCompleted = !task.isCompleted)) },
                        onClick = { taskToDelete = task }
                    )
                }
            }
        }

        // Input Bar
        // No imePadding here, because MainActivity is NOT edge-to-edge.
        // System adjustResize will move this up.
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Введите заметку...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onAddTask(text, isCheckboxMode)
                            text = ""
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Add", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showAddChatDialog) {
        AddChatDialog(
            onDismiss = { showAddChatDialog = false },
            onConfirm = { name, color ->
                onAddChat(name, color)
                showAddChatDialog = false
            }
        )
    }

    if (showDeleteCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedDialog = false },
            title = { Text("Удалить выполненные") },
            text = { Text("Вы уверены, что хотите удалить все завершенные заметки в этом чате?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCompleted()
                    showDeleteCompletedDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Удалить заметку") },
            text = { Text("Удалить '${taskToDelete?.title}'?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteTask(taskToDelete!!)
                    taskToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun AddChatDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(Color.Blue.toArgb()) }
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color.Gray)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый чат") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Введите название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите цвет:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, RoundedCornerShape(4.dp))
                                .border(
                                    if (selectedColor == color.toArgb()) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { selectedColor = color.toArgb() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отменить") }
        }
    )
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    val alpha = if (task.isCompleted) 0.5f else 1f
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.hasCheckbox) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            }
        }
    }
}
