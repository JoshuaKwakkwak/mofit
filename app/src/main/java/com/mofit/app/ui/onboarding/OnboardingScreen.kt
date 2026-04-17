package com.mofit.app.ui.onboarding

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mofit.app.data.model.UserProfile
import kotlinx.coroutines.launch

private val GreenAccent = Color(0xFF4CAF50)
private val GrayColor = Color(0xFF9E9E9E)
private val DarkSurface = Color(0xFF1E1E1E)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    var step by mutableStateOf(0)
    var gender by mutableStateOf("")
    var height by mutableStateOf(170f)
    var weight by mutableStateOf(70f)
    var bodyType by mutableStateOf("")
    var goal by mutableStateOf("")

    private val db get() = (getApplication<Application>() as MofitApplication).database

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            db.workoutDao().upsertProfile(
                UserProfile(
                    gender = gender,
                    heightCm = height.toInt(),
                    weightKg = weight.toInt(),
                    bodyType = bodyType,
                    goal = goal
                )
            )
            onComplete()
        }
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Top
        ) {
            StepIndicator(currentStep = viewModel.step)

            Spacer(modifier = Modifier.height(40.dp))

            when (viewModel.step) {
                0 -> GenderStep(
                    selectedGender = viewModel.gender,
                    onGenderSelected = { viewModel.gender = it }
                )
                1 -> BodyMetricsStep(
                    height = viewModel.height,
                    weight = viewModel.weight,
                    onHeightChange = { viewModel.height = it },
                    onWeightChange = { viewModel.weight = it }
                )
                2 -> BodyTypeStep(
                    selectedBodyType = viewModel.bodyType,
                    onBodyTypeSelected = { viewModel.bodyType = it }
                )
                3 -> GoalStep(
                    goal = viewModel.goal,
                    onGoalChange = { viewModel.goal = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isEnabled = when (viewModel.step) {
                0 -> viewModel.gender.isNotEmpty()
                2 -> viewModel.bodyType.isNotEmpty()
                3 -> viewModel.goal.isNotEmpty()
                else -> true
            }

            Button(
                onClick = {
                    if (viewModel.step < 3) {
                        viewModel.step++
                    } else {
                        viewModel.save(onComplete)
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = GrayColor,
                    disabledContentColor = Color.Black
                )
            ) {
                Text(
                    text = if (viewModel.step < 3) "다음" else "시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index <= currentStep) GreenAccent else GrayColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun GenderStep(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "모핏에 오신 걸 환영합니다",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "나만의 AI 트레이너",
            color = GrayColor,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("남성", "여성").forEach { label ->
                val isSelected = selectedGender == label
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) GreenAccent else GrayColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onGenderSelected(label) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) GreenAccent else GrayColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyMetricsStep(
    height: Float,
    weight: Float,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "신체 정보를 입력해주세요",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "키: ${height.toInt()}cm",
                color = Color.White,
                fontSize = 16.sp
            )
            Slider(
                value = height,
                onValueChange = onHeightChange,
                valueRange = 140f..200f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = GreenAccent,
                    activeTrackColor = GreenAccent,
                    inactiveTrackColor = GrayColor
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "체중: ${weight.toInt()}kg",
                color = Color.White,
                fontSize = 16.sp
            )
            Slider(
                value = weight,
                onValueChange = onWeightChange,
                valueRange = 40f..130f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = GreenAccent,
                    activeTrackColor = GreenAccent,
                    inactiveTrackColor = GrayColor
                )
            )
        }
    }
}

@Composable
private fun BodyTypeStep(
    selectedBodyType: String,
    onBodyTypeSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "체형을 선택하세요",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("슬림", "보통", "근육형").forEach { label ->
                val isSelected = selectedBodyType == label
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) GreenAccent else GrayColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onBodyTypeSelected(label) }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) GreenAccent else GrayColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalStep(
    goal: String,
    onGoalChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = "목표를 입력하세요",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = goal,
            onValueChange = onGoalChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "예: 3개월 안에 스쿼트 100개",
                    color = GrayColor
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GreenAccent,
                unfocusedBorderColor = GrayColor,
                cursorColor = GreenAccent,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(14.dp)
        )
    }
}
