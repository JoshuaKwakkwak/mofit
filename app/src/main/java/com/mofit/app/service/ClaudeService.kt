package com.mofit.app.service

import com.mofit.app.data.UserProfile
import com.mofit.app.data.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CoachingType { PRE_WORKOUT, POST_WORKOUT }

class ClaudeService {

    companion object {
        const val API_KEY = "YOUR_CLAUDE_API_KEY"
    }

    private val client = OkHttpClient()

    suspend fun getCoaching(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>,
        type: CoachingType
    ): String = withContext(Dispatchers.IO) {
        val profileInfo = "성별=${profile.gender}, 키=${profile.heightCm}cm, 체중=${profile.weightKg}kg, 체형=${profile.bodyType}, 목표=${profile.goal}"
        val historyInfo = recentSessions.take(7).joinToString("\n") { s ->
            val dateStr = SimpleDateFormat("M/d", Locale.KOREAN).format(Date(s.date))
            "${dateStr}: 스쿼트 ${s.totalReps}회 ${s.setCount}세트"
        }
        val suffix = when (type) {
            CoachingType.PRE_WORKOUT -> "위 정보를 바탕으로 오늘 운동 전 조언을 한국어로 2-3문장으로 해줘. 격려하고 목표에 맞게 구체적으로."
            CoachingType.POST_WORKOUT -> "위 최근 기록을 바탕으로 운동 후 피드백을 한국어로 2-3문장으로 해줘. 노력을 인정하고 개선점을 제안해줘."
        }
        val prompt = "사용자 정보: $profileInfo\n최근 운동 기록:\n$historyInfo\n\n$suffix"

        val bodyJson = """{"model":"claude-haiku-4-5-20251001","max_tokens":300,"messages":[{"role":"user","content":"${prompt.replace("\"", "\\'")}"}]}"""
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext ""
        val textStart = body.indexOf("\"text\":\"") + 8
        val textEnd = body.indexOf("\"", textStart)
        if (textStart > 7 && textEnd > textStart) body.substring(textStart, textEnd) else ""
    }
}
