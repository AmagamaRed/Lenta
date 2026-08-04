package com.example.lenta.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lenta.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * from tasks WHERE deletedAt IS NULL ORDER BY startTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * from tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedTasks(): Flow<List<Task>>

    @Query("SELECT * from tasks WHERE id = :id AND deletedAt IS NULL")
    fun getTask(id: Long): Flow<Task>
    
    @Query("SELECT * FROM tasks WHERE startTime >= :startOfDay AND startTime <= :endOfDay AND deletedAt IS NULL")
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun deleteOldDeletedTasks(threshold: Long)
}