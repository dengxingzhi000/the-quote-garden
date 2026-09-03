package com.dailymind.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val Xs: Dp = 4.dp,
    val Sm: Dp = 8.dp,
    val Md: Dp = 16.dp,
    val Lg: Dp = 24.dp,
    val Xl: Dp = 32.dp,
    val Xxl: Dp = 48.dp,
    val Hero: Dp = 64.dp
)

val EditorialSpacing = Spacing()

val LocalSpacing = staticCompositionLocalOf { Spacing() }
