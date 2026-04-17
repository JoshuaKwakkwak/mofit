package com.mofit.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id=1")
    fun getProfile(): Flow<UserProfile?>

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_session ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE date > :startOfDay ORDER BY date DESC")
    fun getTodaySessions(startOfDay: Long): Flow<List<WorkoutSession>>
}
