package com.mofit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val gender: String,
    val heightCm: Int,
    val weightKg: Int,
    val bodyType: String,
    val goal: String,
    val createdAt: Long = System.currentTimeMillis()
)
