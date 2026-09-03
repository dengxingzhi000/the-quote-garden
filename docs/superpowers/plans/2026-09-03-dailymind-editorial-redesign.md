# DailyMind Editorial Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Home + Favorite + Theme in Pure Editorial Typography (quiet luxury) with zero business-logic changes.

**Architecture:** Tokens first (`Spacing`, quiet-luxury color schemes, downloadable serif/sans typography with system fallback), then shared `EditorialComponents`, then Home and Favorite screens on top. ViewModels, repositories, Room, WorkManager, navigation are untouched.

**Tech Stack:** Jetpack Compose (BOM 2025.10.01) + Material3, `androidx.compose.ui:ui-text-google-fonts` (Play Services font provider, minSdk 24), Hilt, JUnit4 + MockK (existing test setup).

**Spec:** `docs/superpowers/specs/2026-09-03-dailymind-editorial-redesign-design.md`

**Known boundary (do NOT fix):** `FavoriteViewModel.favorites` maps everything through `.filter { _ -> false }`, so the list is always empty until someone fixes the ViewModel separately. The Favorite empty state is therefore the primary path — build it well.

---

### Task 1: Add Google Fonts downloadable-fonts dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add catalog entry**

```toml
androidx-compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
```

Place it directly after the `androidx-compose-ui` line (line 21) in `gradle/libs.versions.toml`.

- [ ] **Step 2: Add implementation dependency**

```kotlin
implementation(libs.androidx.compose.ui.text.google.fonts)
```

Place it directly after `implementation(libs.androidx.compose.ui)` (line 25) in `app/build.gradle.kts`.

- [ ] **Step 3: Verify dependency resolves**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i "ui-text-google-fonts"`
Expected: a line showing `androidx.compose.ui:ui-text-google-fonts` resolved from the BOM.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "feat: add ui-text-google-fonts for downloadable editorial fonts"
```

---

### Task 2: Spacing tokens (TDD)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/designsystem/Spacing.kt`
- Test: `app/src/test/java/com/dailymind/core/designsystem/SpacingTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dailymind.core.designsystem

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacingTest {
    @Test fun `spacing scale matches editorial spec`() {
        assertEquals(4.dp, EditorialSpacing.Xs)
        assertEquals(8.dp, EditorialSpacing.Sm)
        assertEquals(16.dp, EditorialSpacing.Md)
        assertEquals(24.dp, EditorialSpacing.Lg)
        assertEquals(32.dp, EditorialSpacing.Xl)
        assertEquals(48.dp, EditorialSpacing.Xxl)
        assertEquals(64.dp, EditorialSpacing.Hero)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.core.designsystem.SpacingTest"`
Expected: FAIL — `Unresolved reference: EditorialSpacing`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.core.designsystem.SpacingTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/Spacing.kt app/src/test/java/com/dailymind/core/designsystem/SpacingTest.kt
git commit -m "feat: add editorial spacing tokens"
```

---

### Task 3: Quiet-luxury color schemes (TDD)

**Files:**
- Create: `app/src/main/java/com/dailymind/core/designsystem/theme/EditorialColors.kt`
- Test: `app/src/test/java/com/dailymind/core/designsystem/EditorialColorsTest.kt`

Rationale for a separate file: keeps the new schemes reviewable next to the old `Theme.kt` palette until `Theme.kt` is rewired in Task 5.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.dailymind.core.designsystem

import com.dailymind.core.designsystem.theme.LightQuietLuxury
import com.dailymind.core.designsystem.theme.DarkQuietLuxury
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialColorsTest {
    @Test fun `light scheme matches spec hex`() {
        assertEquals(0xFFF7F5F0u, LightQuietLuxury.background.value)
        assertEquals(0xFF171717u, LightQuietLuxury.onBackground.value)
        assertEquals(0xFF77736Bu, LightQuietLuxury.onSurfaceVariant.value)
        assertEquals(0xFFDDD9D0u, LightQuietLuxury.outlineVariant.value)
        assertEquals(0xFF8A5A44u, LightQuietLuxury.primary.value)
    }

    @Test fun `dark scheme matches spec hex`() {
        assertEquals(0xFF151515u, DarkQuietLuxury.background.value)
        assertEquals(0xFFF2EFE8u, DarkQuietLuxury.onBackground.value)
        assertEquals(0xFFA7A39Bu, DarkQuietLuxury.onSurfaceVariant.value)
        assertEquals(0xFF383838u, DarkQuietLuxury.outlineVariant.value)
        assertEquals(0xFFC49A7Au, DarkQuietLuxury.primary.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.core.designsystem.EditorialColorsTest"`
Expected: FAIL — `Unresolved reference: LightQuietLuxury`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.dailymind.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightQuietLuxury = lightColorScheme(
    primary = Color(0xFF8A5A44),
    onPrimary = Color(0xFFF7F5F0),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF171717),
    surface = Color(0xFFF7F5F0),
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF7F5F0),
    onSurfaceVariant = Color(0xFF77736B),
    outline = Color(0xFFDDD9D0),
    outlineVariant = Color(0xFFDDD9D0),
    error = Color(0xFF9C3B2E),
    onError = Color(0xFFF7F5F0)
)

val DarkQuietLuxury = darkColorScheme(
    primary = Color(0xFFC49A7A),
    onPrimary = Color(0xFF151515),
    background = Color(0xFF151515),
    onBackground = Color(0xFFF2EFE8),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFF2EFE8),
    surfaceVariant = Color(0xFF151515),
    onSurfaceVariant = Color(0xFFA7A39B),
    outline = Color(0xFF383838),
    outlineVariant = Color(0xFF383838),
    error = Color(0xFFD18A7A),
    onError = Color(0xFF151515)
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.core.designsystem.EditorialColorsTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/theme/EditorialColors.kt app/src/test/java/com/dailymind/core/designsystem/EditorialColorsTest.kt
git commit -m "feat: add quiet-luxury light and dark color schemes"
```

---

### Task 4: Editorial typography + font-provider manifest

**Files:**
- Create: `app/src/main/java/com/dailymind/core/designsystem/theme/EditorialTypography.kt`
- Create: `app/src/main/res/values/font_certs.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create typography with system fallback**

```kotlin
package com.dailymind.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.dailymind.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val serifDownloadable = FontFamily(
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Noto Serif SC"), fontProvider = fontProvider, weight = FontWeight.Normal)
)

private val sansDownloadable = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider, weight = FontWeight.Normal)
)

val EditorialSerif: FontFamily = serifDownloadable
val EditorialSerifFallback: FontFamily = FontFamily.Serif
val EditorialSansFallback: FontFamily = FontFamily.SansSerif

val EditorialTypography = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = EditorialSansFallback, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.3.sp)
)
```

Offline-first rule: every screen uses `EditorialSerif` only for latin display text and quote text; if fonts are not yet downloaded, Compose falls back silently per-font. Chinese quote text renders in system Serif until Noto Serif SC arrives. Never block content on font download.

- [ ] **Step 2: Add font-provider certs**

Create `app/src/main/res/values/font_certs.xml` with the standard Play Services font-provider certificates. Copy the `<array name="com_google_android_gms_fonts_certs">` block verbatim from the official downloadable-fonts guide (https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts) — the cert bytes are too long to reproduce here, and the doc version is authoritative.

- [ ] **Step 3: Add provider query to the manifest**

Insert inside the top-level `<manifest>` element (sibling of `<application>`, after line 2):

```xml
<queries>
    <provider android:authorities="com.google.android.gms.fonts">
        <package android:name="com.google.android.gms" />
    </provider>
</queries>
```

- [ ] **Step 4: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Font download itself is verified manually on device in Task 9.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/theme/EditorialTypography.kt app/src/main/res/values/font_certs.xml app/src/main/AndroidManifest.xml
git commit -m "feat: add editorial typography with downloadable fonts"
```

---

### Task 5: Shared editorial components + rewire Theme

**Files:**
- Create: `app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt`
- Modify: `app/src/main/java/com/dailymind/core/designsystem/theme/Theme.kt:1-77`

- [ ] **Step 1: Create components (full file)**

```kotlin
package com.dailymind.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailymind.core.model.Quote

@Composable
fun EditorialTopBar(date: String, greeting: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun QuoteBlock(quote: Quote, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = quote.content,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))
        quote.translation?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        quote.author?.let {
            Text(
                text = "— $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        quote.category?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "#$it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OutlineTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun IndexRow(number: String, content: String, author: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onClick, role = Role.Button, interactionSource = interaction, indication = null)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                author?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "— $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
fun EditorialEmpty(title: String, body: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextAction(label = actionLabel, onClick = onAction)
    }
}

@Composable
fun EditorialError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp)) {
        Text(
            text = "Something went quiet",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextAction(label = "Retry →", onClick = onRetry)
    }
}
```

Design rules enforced: 0dp card radius nowhere, 1dp dividers only, 48dp min targets, no `Brush`, no shadows, `tween` reserved for screens (color fade handled by TextButton defaults).

- [ ] **Step 2: Rewire Theme.kt to the new system**

Replace the body of `app/src/main/java/com/dailymind/core/designsystem/theme/Theme.kt` so `DailyMindTheme` uses the new tokens and provides spacing:

```kotlin
package com.dailymind.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dailymind.core.designsystem.LocalSpacing
import com.dailymind.core.designsystem.Spacing

@Composable
fun DailyMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkQuietLuxury else LightQuietLuxury,
        typography = EditorialTypography,
        content = {
            CompositionLocalProvider(LocalSpacing provides Spacing()) {
                content()
            }
        }
    )
}
```

Delete the old `LightColors`, `DarkColors`, and `AppTypography` from the file. Keep the `DailyMindTheme(darkTheme, content)` signature so `MainActivity` needs no change for theming.

- [ ] **Step 3: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run existing unit tests (regression)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests (`HomeViewModelTest`, `QuoteRepositoryTest`, `RouteTest`, `QuoteTest`, `PreferencesTest`, `ApiServiceTest`, `DailySyncWorkerTest`, `ScaffoldingTest`, `QuoteDaoTest` where applicable) pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dailymind/core/designsystem/EditorialComponents.kt app/src/main/java/com/dailymind/core/designsystem/theme/Theme.kt
git commit -m "feat: add editorial components and rewire theme"
```

---

### Task 6: Rebuild HomeScreen (same events, no logic change)

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/home/HomeScreen.kt:1-271`

- [ ] **Step 1: Rewrite HomeScreen editorially**

Full replacement. Same `viewModel: HomeViewModel = hiltViewModel()` and `onNavigateToFavorite: () -> Unit` signature. Same events: `HomeEvent.Load`, `HomeEvent.NextRandom`, `HomeEvent.Favorite(q.id)`. Layout: `LazyColumn` with 24dp horizontal padding, 64dp hero top, `EditorialTopBar` (date formatted `MM / dd` + greeting by hour), section label `TODAY'S QUOTE`, `QuoteBlock`, actions row (`TextAction("Explore  →")` for next, `OutlineTextAction("+ Saved")` for favorite + wiring `onNavigateToFavorite` to a "Saved →" text link), footnote for fallback/cache states, `EditorialEmpty`/`EditorialError` for empty/error, `AnimatedContent` fade 200ms + ≤8dp rise kept, gradient/Card/elevation removed:

```kotlin
package com.dailymind.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.EditorialError
import com.dailymind.core.designsystem.EditorialTopBar
import com.dailymind.core.designsystem.OutlineTextAction
import com.dailymind.core.designsystem.QuoteBlock
import com.dailymind.core.designsystem.TextAction
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFavorite: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd"))
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                EditorialTopBar(date = date, greeting = greeting)
                Spacer(modifier = Modifier.height(48.dp))
            }
            item {
                AnimatedVisibility(visible = state.isLoading) {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
            item {
                AnimatedContent(
                    targetState = state.quote?.id ?: state.error ?: "empty",
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 12 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 12 })
                    },
                    label = "quoteContent"
                ) { _ ->
                    when {
                        state.quote != null -> {
                            val q = state.quote!!
                            Text(
                                text = "TODAY'S QUOTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            QuoteBlock(quote = q)
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TextAction(
                                    label = "Next →",
                                    onClick = { viewModel.onEvent(HomeEvent.NextRandom) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlineTextAction(
                                    label = "+ Favorite",
                                    onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            TextAction(label = "Saved →", onClick = onNavigateToFavorite)
                            if (q.id.startsWith("fallback")) {
                                Text(
                                    text = "Local demo content · syncs when online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                        state.error != null -> {
                            EditorialError(
                                message = state.error!!,
                                onRetry = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
                        !state.isLoading -> {
                            EditorialEmpty(
                                title = "Nothing here yet",
                                body = "Check your connection and try again.",
                                actionLabel = "Retry →",
                                onAction = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Offline-first · cached first, syncs quietly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
```

Note: `java.time` requires API 26+ desugaring for minSdk 24 — AGP 9 with `coreLibraryDesugaring` is the standard fix; if the build fails on `LocalDate`, add desugaring in this task (see fallback in the troubleshooting note of Task 9). Preferred: keep `java.time` and add desugaring only if needed.

- [ ] **Step 2: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run unit tests (regression — ViewModel untouched)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.feature.home.HomeViewModelTest"`
Expected: BUILD SUCCESSFUL, test passes.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/home/HomeScreen.kt
git commit -m "feat: rebuild Home in editorial typography"
```

---

### Task 7: Rebuild FavoriteScreen (same flow, no logic change)

**Files:**
- Modify: `app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt:1-15`

- [ ] **Step 1: Rewrite FavoriteScreen as editorial index**

Same `vm: FavoriteViewModel = hiltViewModel()` signature, same `favorites` flow. Numbered `IndexRow` list with dividers, header, count, empty state:

```kotlin
package com.dailymind.feature.favorite

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    vm: FavoriteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "Collection",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A small archive of beautiful thoughts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "${list.size} saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (list.isEmpty()) {
                item {
                    EditorialEmpty(
                        title = "No saved quotes yet",
                        body = "Keep the lines that stay with you.",
                        actionLabel = "Explore →",
                        onAction = onNavigateBack
                    )
                }
            } else {
                itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
                    IndexRow(
                        number = (index + 1).toString().padStart(2, '0'),
                        content = q.content,
                        author = q.author,
                        onClick = {}
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
```

`onNavigateBack` defaults to no-op so existing `AppNavDisplay` call `FavoriteScreen()` keeps compiling; wire it to back navigation in Task 8.

- [ ] **Step 2: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dailymind/feature/favorite/FavoriteScreen.kt
git commit -m "feat: rebuild Favorites as editorial index"
```

---

### Task 8: Edge-to-edge + back wiring

**Files:**
- Modify: `app/src/main/java/com/dailymind/MainActivity.kt:1-16`
- Modify: `app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt:11-25`

- [ ] **Step 1: Enable edge-to-edge in MainActivity**

```kotlin
package com.dailymind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dailymind.core.designsystem.theme.DailyMindTheme
import com.dailymind.core.navigation.AppNavDisplay
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DailyMindTheme { AppNavDisplay() } }
    }
}
```

`Scaffold` in both screens already applies `padding` from `WindowInsets` via the `padding` parameter, so no content hides behind system bars.

- [ ] **Step 2: Wire Favorite back navigation**

In `AppNavDisplay.kt`, change the Favorite entry to pass back navigation:

```kotlin
is Route.Favorite -> NavEntry(route) { FavoriteScreen(onNavigateBack = { backStack.removeLastOrNull() }) }
```

(Home already exposes `onNavigateToFavorite`; no change needed there.)

- [ ] **Step 3: Verify build + navigation test**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

Run: `./gradlew :app:testDebugUnitTest --tests "com.dailymind.core.navigation.RouteTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dailymind/MainActivity.kt app/src/main/java/com/dailymind/core/navigation/AppNavDisplay.kt
git commit -m "feat: edge-to-edge and favorite back navigation"
```

---

### Task 9: MASTER override note + full verification

**Files:**
- Modify: `design-system/MASTER.md:1-12`

- [ ] **Step 1: Append override note at the top of MASTER.md**

```markdown
> **OVERRIDE (2026-09-03):** For Home, Favorite, and Theme, the quiet-luxury spec in
> `docs/superpowers/specs/2026-09-03-dailymind-editorial-redesign-design.md` supersedes
> this file (palette, typography, dark-mode rule). This Master file remains in force
> for all other pages until migrated.
```

Insert directly below line 5 (`> If not, strictly follow the rules below.`), before the `---` on line 7.

- [ ] **Step 2: Full build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Full unit-test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including new `SpacingTest`, `EditorialColorsTest` and existing `HomeViewModelTest`, `QuoteRepositoryTest`, `RouteTest`).

- [ ] **Step 4: Manual verification checklist (on device/emulator)**

  - Light mode: Home reads date → serif greeting → quote → divider → meta → text actions; zero cards/shadows/gradients.
  - Dark mode: same hierarchy, no pure-black crush, body text ≥ 4.5:1.
  - Widths 360dp + 410dp: no overflow, no horizontal scroll.
  - Font scale 1.0x + 1.3x: no clipped actions, targets still ≥ 48dp.
  - States: quote switch fades; error retries via `HomeEvent.Load`; empty favorites shows serif empty + `Explore →`.
  - Motion: with system animations disabled, all content remains fully readable (rise is ≤8dp decorative only, never carries meaning).
  - Fonts: first run shows system fallback, Noto Serif SC/Inter arrive after download (Play Services present); offline device stays readable.

- [ ] **Step 5: Commit**

```bash
git add design-system/MASTER.md
git commit -m "docs: note quiet-luxury override for Home Favorite Theme"
```

**Troubleshooting note:** if Task 6 fails to compile on `java.time` with minSdk 24, add core-library desugaring in that task instead of changing screen code: in `app/build.gradle.kts` add `coreLibraryDesugaring(libs.desugaring)` + `compileOptions { isCoreLibraryDesugaringEnabled = true }` with catalog entry `desugaring = { group = "com.android.tools", name = "desugar_jdk_libs", version = "2.1.4" }`, then re-run `./gradlew :app:assembleDebug`.
