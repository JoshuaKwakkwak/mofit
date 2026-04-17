package com.mofit.app.ui.tracking

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mofit.app.MofitApplication
import com.mofit.app.data.model.JointPoint
import com.mofit.app.data.model.PoseResult
import com.mofit.app.data.model.WorkoutSession
import com.mofit.app.data.model.WorkoutSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class WorkoutPhase {
    object Idle : WorkoutPhase()
    object PalmDetection : WorkoutPhase()
    data class Countdown(val seconds: Int) : WorkoutPhase()
    object Tracking : WorkoutPhase()
    object Finished : WorkoutPhase()
}

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    var phase by mutableStateOf<WorkoutPhase>(WorkoutPhase.Idle)
    var completedSets = mutableStateListOf<WorkoutSet>()
    var currentJoints by mutableStateOf<List<JointPoint>>(emptyList())
    var currentRepCount by mutableStateOf(0)
    var wristHoldProgress by mutableStateOf(0f)

    private val db get() = (getApplication<MofitApplication>()).database
    private var countdownJob: Job? = null

    val totalReps: Int get() = completedSets.sumOf { it.reps }

    fun onPoseResult(result: PoseResult) {
        currentJoints = result.joints
        currentRepCount = result.repCount
        if (result.isWristRaised) {
            when (phase) {
                is WorkoutPhase.PalmDetection -> startCountdown()
                is WorkoutPhase.Tracking -> completeSet(result.repCount)
                else -> {}
            }
        }
    }

    fun startSession() {
        phase = WorkoutPhase.PalmDetection
    }

    fun startCountdown() {
        if (phase !is WorkoutPhase.PalmDetection) return
        countdownJob?.cancel()
        phase = WorkoutPhase.Countdown(5)
        countdownJob = viewModelScope.launch {
            for (i in 4 downTo 1) {
                delay(1000)
                phase = WorkoutPhase.Countdown(i)
            }
            delay(1000)
            phase = WorkoutPhase.Tracking
        }
    }

    fun completeSet(reps: Int) {
        if (phase !is WorkoutPhase.Tracking) return
        val set = WorkoutSet(
            setNumber = completedSets.size + 1,
            reps = reps,
            completedAt = System.currentTimeMillis()
        )
        completedSets.add(set)
        phase = WorkoutPhase.PalmDetection
    }

    fun endWorkout() {
        countdownJob?.cancel()
        phase = WorkoutPhase.Finished
    }

    fun saveAndClose(onDone: () -> Unit) {
        val gson = Gson()
        val session = WorkoutSession(
            totalReps = completedSets.sumOf { it.reps },
            setCount = completedSets.size,
            setsJson = gson.toJson(completedSets.toList())
        )
        viewModelScope.launch {
            db.workoutDao().insertSession(session)
            onDone()
        }
    }
}
