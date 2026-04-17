package com.mofit.app.model

import java.io.Serializable

data class WorkoutSet(
    val setNumber: Int,
    val reps: Int,
    val completedAt: Long
) : Serializable
