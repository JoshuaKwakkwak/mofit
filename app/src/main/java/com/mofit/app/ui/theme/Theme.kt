package com.mofit.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MofitColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1A1A1A)
    val Border = Color(0xFF404040)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF999999)
    val Accent = Color(0xFF33CC66)
}

private val MofitColorScheme = darkColorScheme(
    background = MofitColors.Background,
    surface = MofitColors.Surface,
    primary = MofitColors.Accent,
    onBackground = MofitColors.TextPrimary,
    onSurface = MofitColors.TextPrimary
)

@Composable
fun MofitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MofitColorScheme) {
        Surface(color = MofitColors.Background, content = content)
    }
}

fun Modifier.cardStyle(): Modifier =
    this.background(MofitColors.Surface, RoundedCornerShape(12.dp)).padding(16.dp)
