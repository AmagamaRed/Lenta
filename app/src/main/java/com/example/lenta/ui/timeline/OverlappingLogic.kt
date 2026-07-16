package com.example.lenta.ui.timeline

import com.example.lenta.model.Task

data class TaskPosition(
    val task: Task,
    val lane: Int,
    val totalLanes: Int
)

fun calculateTaskPositions(tasks: List<Task>): List<TaskPosition> {
    if (tasks.isEmpty()) return emptyList()

    val allDayTasks = tasks.filter { it.isAllDay }.sortedBy { it.createdAt }
    val timedTasks = tasks.filter { !it.isAllDay && it.startTime != null && it.endTime != null }
        .sortedBy { it.startTime }

    val positions = mutableListOf<TaskPosition>()
    
    // Add all day tasks first
    allDayTasks.forEachIndexed { index, task ->
        positions.add(TaskPosition(task, index, 1))
    }

    val allDayCount = allDayTasks.size
    val activeGroups = mutableListOf<MutableList<Task>>()

    // Group overlapping tasks
    for (task in timedTasks) {
        var addedToGroup = false
        val taskEffectiveStart = task.startTime!! - task.travelTimeBeforeMs
        val taskEffectiveEnd = task.endTime!! + task.travelTimeAfterMs

        for (group in activeGroups) {
            val lastTaskInGroup = group.maxByOrNull { it.endTime!! + it.travelTimeAfterMs }!!
            val groupEffectiveEnd = lastTaskInGroup.endTime!! + lastTaskInGroup.travelTimeAfterMs
            
            if (taskEffectiveStart < groupEffectiveEnd) {
                group.add(task)
                addedToGroup = true
                break
            }
        }
        if (!addedToGroup) {
            activeGroups.add(mutableListOf(task))
        }
    }

    // Assign lanes within each group
    for (group in activeGroups) {
        val lanes = mutableListOf<Long>() // Stores end times of tasks in each lane
        val groupPositions = mutableListOf<Pair<Task, Int>>()
        
        for (task in group) {
            var assignedLane = -1
            val taskStart = task.startTime!! - task.travelTimeBeforeMs
            val taskEnd = task.endTime!! + task.travelTimeAfterMs

            for (i in lanes.indices) {
                if (taskStart >= lanes[i]) {
                    lanes[i] = taskEnd
                    assignedLane = i
                    break
                }
            }
            if (assignedLane == -1) {
                lanes.add(taskEnd)
                assignedLane = lanes.size - 1
            }
            groupPositions.add(task to assignedLane)
        }
        
        val totalLanesInGroup = lanes.size
        for ((task, lane) in groupPositions) {
            positions.add(TaskPosition(task, lane + allDayCount, totalLanesInGroup))
        }
    }

    return positions
}
