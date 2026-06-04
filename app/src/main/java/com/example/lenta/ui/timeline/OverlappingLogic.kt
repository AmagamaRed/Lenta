package com.example.lenta.ui.timeline

import com.example.lenta.model.Task

data class TaskPosition(
    val task: Task,
    val lane: Int,
    val totalLanes: Int
)

fun calculateTaskPositions(tasks: List<Task>): List<TaskPosition> {
    if (tasks.isEmpty()) return emptyList()

    // Filter tasks that have time and sort them by start time
    val timedTasks = tasks.filter { it.startTime != null && it.endTime != null }
        .sortedBy { it.startTime }

    val positions = mutableListOf<TaskPosition>()
    val activeGroups = mutableListOf<MutableList<Task>>()

    // Group overlapping tasks
    for (task in timedTasks) {
        var addedToGroup = false
        for (group in activeGroups) {
            val lastTaskInGroup = group.maxByOrNull { it.endTime!! }!!
            if (task.startTime!! < lastTaskInGroup.endTime!!) {
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
            for (i in lanes.indices) {
                if (task.startTime!! >= lanes[i]) {
                    lanes[i] = task.endTime!!
                    assignedLane = i
                    break
                }
            }
            if (assignedLane == -1) {
                lanes.add(task.endTime!!)
                assignedLane = lanes.size - 1
            }
            groupPositions.add(task to assignedLane)
        }
        
        val totalLanesInGroup = lanes.size
        for ((task, lane) in groupPositions) {
            positions.add(TaskPosition(task, lane, totalLanesInGroup))
        }
    }

    return positions
}
