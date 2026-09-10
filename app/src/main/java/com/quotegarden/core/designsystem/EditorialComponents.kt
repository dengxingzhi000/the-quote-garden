@file:Suppress("FunctionName")

package com.quotegarden.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import com.quotegarden.core.model.Quote
import com.quotegarden.R

@Composable
fun EditorialTopBar(
    date: String,
    greeting: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    brandMark: Int? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.invoke()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (brandMark != null) {
                Image(
                    painter = painterResource(id = brandMark),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = greeting,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun QuoteBlock(quote: Quote, modifier: Modifier = Modifier) {
    val segments = remember(quote.id, quote.content) {
        splitQuoteSegments(quote.content).ifEmpty { listOf(quote.content) }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(20.dp))
            }
            Text(
                text = segment,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (!quote.translation.isNullOrBlank() || !quote.author.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        }
        if (!quote.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = quote.translation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic
            )
        }
        if (!quote.author.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "— ${quote.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@Composable
fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = interaction,
                indication = null
            )
            .padding(vertical = 8.dp)
    )
}

@Composable
fun OutlineTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOn: Boolean = false,
    pulseKey: Int = 0,
) {
    val outline = if (isOn) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            scale.animateTo(0.96f, tween(80))
            scale.animateTo(1f, tween(160))
        }
    }
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background, shape)
            .border(BorderStroke(1.dp, outline), shape)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isOn) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun IndexRow(
    number: String,
    content: String,
    author: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeOut: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    if (onSwipeOut != null) {
        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onSwipeOut()
                    true
                } else false
            }
        )
        SwipeToDismissBox(
            state = state,
            backgroundContent = { },
            modifier = modifier.fillMaxWidth()
        ) {
            RowContent(number, content, author, onClick, interaction)
        }
    } else {
        RowContent(number, content, author, onClick, interaction, modifier)
    }
}

@Composable
private fun RowContent(
    number: String,
    content: String,
    author: String?,
    onClick: () -> Unit,
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interaction,
                indication = null
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (number.isNotEmpty()) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            author?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "-- $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = ">",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
}

@Composable
fun EditorialEmpty(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: Int? = null,
    illustrationSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(id = illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(illustrationSize)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextAction(label = actionLabel, onClick = onAction)
    }
}

@Composable
fun EditorialError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: Int? = null,
    illustrationSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(id = illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(illustrationSize)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = "Something went quiet",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextAction(label = "Retry", onClick = onRetry)
    }
}

data class BottomTab(val id: String, val label: String)

@Composable
fun EditorialBottomBar(
    tabs: List<BottomTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val selected = tab.id == selectedId
                TextButton(
                    onClick = { onSelect(tab.id) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RectangleShape
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
fun HomeActionsBar(
    saveLabel: String,
    isSaved: Boolean,
    favoriteTapKey: Int,
    onSave: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlineTextAction(
                    label = saveLabel,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    isOn = isSaved,
                    pulseKey = favoriteTapKey,
                )
                OutlineTextAction(
                    label = "Next →",
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
