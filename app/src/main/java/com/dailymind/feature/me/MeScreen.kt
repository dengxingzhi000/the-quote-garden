package com.dailymind.feature.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow
import com.dailymind.core.designsystem.SettingRow

private const val APP_VERSION = "0.1.0-beta.1"

@Composable
fun MeScreen(
    vm: MeViewModel = hiltViewModel(),
    onExplore: () -> Unit = {}
) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    var showTheme by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    if (showTheme) {
        ThemeDialog(
            current = mode,
            onPick = { vm.setThemeMode(it); showTheme = false },
            onDismiss = { showTheme = false }
        )
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Text(text = "Me", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${list.size} saved · DailyMind $APP_VERSION", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "SAVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (list.isEmpty()) {
                item {
                    EditorialEmpty(title = "No saved quotes yet", body = "Keep the lines that stay with you.", actionLabel = "Explore →", onAction = onExplore)
                }
            } else {
                itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
                    IndexRow(number = (index + 1).toString().padStart(2, '0'), content = q.content, author = q.author, onClick = {}, onSwipeOut = { vm.onEvent(MeEvent.Unfavorite(q.id)) })
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "SETTINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                SettingRow(label = "Theme", value = mode.label(), onClick = { showTheme = true })
                SettingRow(label = "About", value = null, onClick = { showAbout = true })
                Spacer(modifier = Modifier.height(32.dp))
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
private fun ThemeDialog(current: ThemeMode, onPick: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(text = "Theme", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                ThemeMode.entries.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(m) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = m == current, onClick = { onPick(m) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = m.label(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
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
                Text(text = "Close", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        },
        title = { Text(text = "DailyMind", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(text = "Offline-first daily quotes. Version $APP_VERSION.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}
