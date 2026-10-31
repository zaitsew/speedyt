package com.fabriquefutur.speedyt.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.fabriquefutur.speedyt.analysis.BufferbloatGrade

/** Dark, high-contrast palette for reading from a sofa. */
object SpeedytColors {
    val Background = Color(0xFF0B0F14)
    val Card = Color(0xFF151B23)
    val CardHigh = Color(0xFF212B38)
    val Text = Color(0xFFF2F5F8)
    val Muted = Color(0xFFB3BDC8)
    val Track = Color(0xFF2A3442)
    val Download = Color(0xFF2DD4BF)
    val Upload = Color(0xFFC4B5FD)
    val Focus = Color(0xFFFFFFFF)
    val Error = Color(0xFFFCA5A5)

    fun grade(g: BufferbloatGrade): Color = when (g) {
        BufferbloatGrade.A -> Color(0xFF4ADE80)
        BufferbloatGrade.B -> Color(0xFFBEF264)
        BufferbloatGrade.C -> Color(0xFFFBBF24)
        BufferbloatGrade.D -> Color(0xFFF87171)
    }
}

/** TV safe area: 5% of a 960x540 dp screen on each side. */
val SafeHorizontal = 48.dp
val SafeVertical = 27.dp

@Composable
fun SpeedytTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SpeedytColors.Download,
            onPrimary = SpeedytColors.Background,
            background = SpeedytColors.Background,
            onBackground = SpeedytColors.Text,
            surface = SpeedytColors.Card,
            onSurface = SpeedytColors.Text,
            surfaceVariant = SpeedytColors.CardHigh,
            onSurfaceVariant = SpeedytColors.Muted,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(SpeedytColors.Background)) { content() }
    }
}
