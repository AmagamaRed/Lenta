package com.example.lenta.data

import androidx.room.TypeConverter
import com.example.lenta.model.Importance
import com.example.lenta.model.Recurrence

class Converters {
    @TypeConverter
    fun fromImportance(importance: Importance): String {
        return importance.name
    }

    @TypeConverter
    fun toImportance(value: String?): Importance {
        return try {
            Importance.valueOf(value ?: "MEDIUM")
        } catch (e: Exception) {
            Importance.MEDIUM
        }
    }

    @TypeConverter
    fun fromImportanceInt(value: Int): Importance {
        return Importance.entries.getOrElse(value) { Importance.MEDIUM }
    }

    @TypeConverter
    fun importanceToInt(importance: Importance): Int {
        return importance.ordinal
    }

    @TypeConverter
    fun fromRecurrence(recurrence: Recurrence): String {
        return recurrence.name
    }

    @TypeConverter
    fun toRecurrence(value: String?): Recurrence {
        return try {
            Recurrence.valueOf(value ?: "NONE")
        } catch (e: Exception) {
            Recurrence.NONE
        }
    }

    @TypeConverter
    fun fromRecurrenceInt(value: Int): Recurrence {
        return Recurrence.entries.getOrElse(value) { Recurrence.NONE }
    }

    @TypeConverter
    fun recurrenceToInt(recurrence: Recurrence): Int {
        return recurrence.ordinal
    }
}