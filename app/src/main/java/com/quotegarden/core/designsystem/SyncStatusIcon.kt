@file:Suppress("FunctionName")

package com.quotegarden.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quotegarden.R

@Composable
fun SyncStatusIcon(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (isLoading) {
        val spin = rememberInfiniteTransition(label = "syncSpin")
        val angle by spin.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "angle"
        )
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_sync_pending_static),
            contentDescription = "Syncing",
            colorFilter = ColorFilter.tint(tint),
            modifier = modifier.size(size).rotate(angle)
        )
    } else {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_sync_done),
            contentDescription = "Up to date",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = modifier.size(size)
        )
    }
}
