package com.mofit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val exerciseType: String = "squat",
    val totalReps: Int,
    val setCount: Int,
    val setsJson: String
)
