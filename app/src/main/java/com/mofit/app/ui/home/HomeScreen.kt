package com.mofit.app.ui.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mofit.app.MofitApplication
import com.mofit.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as MofitApplication).database.workoutDao()

    val todaySessions: StateFlow<List<WorkoutSession>> = dao.getTodaySessions(startOfToday())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val todayTotalReps: StateFlow<Int> = todaySessions
        .map { it.sumOf { s -> s.totalReps } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

private fun Modifier.cardStyle(): Modifier = this
    .background(Color(0xFF1A1A1A), RoundedCornerShape(14.dp))
    .padding(16.dp)

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val todaySessions by viewModel.todaySessions.collectAsState()
    val todayReps by viewModel.todayTotalReps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Mofit",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (todaySessions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardStyle()
            ) {
                Text(
                    text = "오늘 운동",
                    color = Color(0xFF999999),
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${todayReps}회",
                        color = Color(0xFF33CC66),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${todaySessions.sumOf { it.setCount }}세트",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33CC66))
        ) {
            Text(
                text = "운동 시작",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
