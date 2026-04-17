package com.mofit.app.ui.coaching

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.mofit.app.MofitApplication
import com.mofit.app.data.CoachingType
import com.mofit.app.service.ClaudeService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoachingViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as MofitApplication).database.workoutDao()
    private val service = ClaudeService()
    private val prefs = PreferenceDataStoreFactory.create { application.preferencesDataStoreFile("coaching_prefs") }
    private val PRE_DATE_KEY = stringPreferencesKey("pre_date")
    private val POST_DATE_KEY = stringPreferencesKey("post_date")

    private fun todayStr() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val profile = dao.getProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val sessions = dao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var preFeedback by mutableStateOf("")
    var postFeedback by mutableStateOf("")
    var loadingPre by mutableStateOf(false)
    var loadingPost by mutableStateOf(false)
    var preUsedToday by mutableStateOf(false)
    var postUsedToday by mutableStateOf(false)

    init {
        viewModelScope.launch {
            prefs.data.collect { data ->
                preUsedToday = data[PRE_DATE_KEY] == todayStr()
                postUsedToday = data[POST_DATE_KEY] == todayStr()
            }
        }
    }

    fun requestCoaching(type: CoachingType) {
        val p = profile.value ?: return
        val isPost = type == CoachingType.POST_WORKOUT
        if (isPost) loadingPost = true else loadingPre = true
        viewModelScope.launch {
            val result = try {
                service.getCoaching(p, sessions.value, type)
            } catch (e: Exception) {
                "오류가 발생했어요. 잠시 후 다시 시도해주세요."
            }
            if (isPost) {
                postFeedback = result
                postUsedToday = true
                prefs.edit { it[POST_DATE_KEY] = todayStr() }
                loadingPost = false
            } else {
                preFeedback = result
                preUsedToday = true
                prefs.edit { it[PRE_DATE_KEY] = todayStr() }
                loadingPre = false
            }
        }
    }
}

@Composable
fun CoachingScreen(viewModel: CoachingViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI 코칭",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        CoachingCard(
            title = "운동 전 코칭",
            subtitle = "목표와 기록 기반 준비 조언",
            feedback = viewModel.preFeedback,
            isLoading = viewModel.loadingPre,
            isUsed = viewModel.preUsedToday,
            onRequest = { viewModel.requestCoaching(CoachingType.PRE_WORKOUT) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        CoachingCard(
            title = "운동 후 코칭",
            subtitle = "오늘 운동 기반 피드백",
            feedback = viewModel.postFeedback,
            isLoading = viewModel.loadingPost,
            isUsed = viewModel.postUsedToday,
            onRequest = { viewModel.requestCoaching(CoachingType.POST_WORKOUT) }
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "하루에 각 1회 코칭을 받을 수 있어요",
            color = Color(0xFF666666),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CoachingCard(
    title: String,
    subtitle: String,
    feedback: String,
    isLoading: Boolean,
    isUsed: Boolean,
    onRequest: () -> Unit
) {
    val active = !isUsed && !isLoading
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = Color(0xFF999999), fontSize = 13.sp)
        if (feedback.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = feedback, color = Color.White, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRequest,
            enabled = active,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) Color(0xFF33CC66) else Color(0xFF333333),
                contentColor = if (active) Color.Black else Color(0xFF666666),
                disabledContainerColor = Color(0xFF333333),
                disabledContentColor = Color(0xFF666666)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF33CC66),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isUsed) "오늘 사용함" else "코칭 받기",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
