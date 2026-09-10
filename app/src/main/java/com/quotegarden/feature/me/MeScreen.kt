package com.quotegarden.feature.me

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotegarden.BuildConfig
import com.quotegarden.R
import com.quotegarden.core.datastore.ThemeMode
import com.quotegarden.core.model.Quote

private val OnDark = Color.White
private val OnDarkMuted = Color.White.copy(alpha = 0.72f)
private val OnDarkFaint = Color.White.copy(alpha = 0.32f)
private val HeroScrim = Color.Black.copy(alpha = 0.5f)

@Composable
fun MeScreen(
    vm: MeViewModel = hiltViewModel(),
    onExplore: () -> Unit = {}
) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    var showTheme by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showContact by remember { mutableStateOf(false) }
    if (showTheme) {
        ThemeDialog(
            current = mode,
            onPick = { vm.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false }
        )
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showContact) ContactUsDialog(onDismiss = { showContact = false })
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.ic_favorites),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        0f to HeroScrim,
                        0.45f to HeroScrim,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                    Text(
                        text = "Me",
                        style = MaterialTheme.typography.displayLarge,
                        color = OnDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${list.size} saved - Quote Garden ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkMuted
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "SAVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (list.isEmpty()) {
                    item {
                        HeroEmptyBlock(
                            title = "No saved quotes yet",
                            body = "Keep the lines that stay with you.",
                            actionLabel = "Explore ->",
                            onAction = onExplore,
                            illustration = R.drawable.empty_favorites,
                            illustrationSize = 160.dp
                        )
                    }
                } else {
                    itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
                        HeroIndexRow(
                            number = (index + 1).toString().padStart(2, '0'),
                            content = q.content,
                            author = q.author,
                            onSwipeOut = { vm.onEvent(MeEvent.Unfavorite(q.id)) }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HeroSettingRow(label = "Theme", value = mode.label(), onClick = { showTheme = true })
                    HeroSettingRow(label = "Contact us", value = null, onClick = { showContact = true })
                    HeroSettingRow(label = "About", value = null, onClick = { showAbout = true })
                    Spacer(modifier = Modifier
                        .navigationBarsPadding()
                        .height(32.dp))
                }
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun themeIconRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.LIGHT -> R.drawable.ic_theme_light
    ThemeMode.DARK -> R.drawable.ic_theme_dark
    ThemeMode.SYSTEM -> R.drawable.ic_theme_system
}

@Composable
private fun HeroIndexRow(
    number: String,
    content: String,
    author: String?,
    onSwipeOut: (() -> Unit)? = null,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val contentColumn: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 16.dp)) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                color = OnDarkMuted,
                modifier = Modifier.width(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnDark
                )
                author?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-- $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnDarkMuted
                    )
                }
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDark
            )
        }
        HorizontalDivider(color = OnDarkFaint, thickness = 1.dp)
    }
    if (onSwipeOut != null) {
        val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                    onSwipeOut()
                    true
                } else false
            }
        )
        androidx.compose.material3.SwipeToDismissBox(
            state = state,
            backgroundContent = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            contentColumn()
        }
    } else {
        contentColumn()
    }
}

@Composable
private fun HeroSettingRow(label: String, value: String?, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick, role = androidx.compose.ui.semantics.Role.Button, interactionSource = interaction, indication = null)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = OnDark,
                modifier = Modifier.weight(1f)
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnDark
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDark
            )
        }
        HorizontalDivider(color = OnDarkFaint, thickness = 1.dp)
    }
}

@Composable
private fun HeroEmptyBlock(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    illustration: Int? = null,
    illustrationSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
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
            color = OnDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OnDarkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = OnDark,
            modifier = Modifier.clickable(onClick = onAction).padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun ThemeDialog(current: ThemeMode, onPick: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = "Theme", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                ThemeMode.entries.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onPick(m) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = m == current, onClick = { onPick(m) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            imageVector = ImageVector.vectorResource(id = themeIconRes(m)),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = m.label(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Quote Garden",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Text(
                text = "Offline-first daily quotes. Version ${BuildConfig.VERSION_NAME}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
