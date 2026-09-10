package com.quotegarden.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotegarden.R
import com.quotegarden.core.designsystem.SyncStatusIcon
import com.quotegarden.core.model.Quote
import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val HeroOnDark = Color.White
private val HeroOnDarkMuted = Color.White.copy(alpha = 0.72f)
private val HeroOnDarkFaint = Color.White.copy(alpha = 0.32f)
private val HeroScrim = Color.Black.copy(alpha = 0.45f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "%02d / %02d".format(now.monthNumber, now.dayOfMonth)
    val greeting = when (now.hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Swipe direction for direction-aware transitions (-1 = next/content moves left,
    // +1 = prev/content moves right), mirroring pager behavior in large reader apps.
    var swipeDirection by remember { mutableStateOf(-1) }
    // Live finger offset: the quote card follows the finger during a horizontal drag
    // (ViewPager2/HorizontalPager feel) instead of only reacting on release.
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val canGoPrev = state.historyIndex > 0

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.ic_welcome),
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

Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val currentQuoteForBar = state.quote
                if (currentQuoteForBar != null) {
                    val qb = currentQuoteForBar
                    HomeActionsBarOnDark(
                        saveLabel = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
                        isSaved = state.isCurrentQuoteFavorite,
                        favoriteTapKey = state.favoriteTapKey,
                        onSave = { viewModel.onEvent(HomeEvent.Favorite(qb.id)) },
                        onNext = { viewModel.onEvent(HomeEvent.NextRandom) },
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                // Header stays pinned: horizontal swipes only swap the quote card below,
                // never the greeting (pager-style chrome, like WeRead/Kindle readers).
                Spacer(modifier = Modifier.height(64.dp))
                Column {
                    HeroTopBar(
                        date = date,
                        greeting = greeting,
                        action = { SyncStatusIcon(isLoading = state.isLoading, tint = HeroOnDarkMuted) }
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    CategoryChips(
                        selected = state.selectedCategory,
                        available = state.availableCategories,
                        onPick = { viewModel.onEvent(HomeEvent.SelectCategory(it)) },
                        onMore = { /* future: show full sheet */ }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (!state.isBrowsing) {
                        AnimatedVisibility(visible = state.quote != null) {
                            Column {
                                Text(
                                    text = "TODAY'S QUOTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HeroOnDarkMuted
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = state.isLoading) {
                    val spin = rememberInfiniteTransition(label = "loadingSpin")
                    val angle by spin.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                        label = "angle"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Image(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_quiet_loading),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(HeroOnDark),
                            modifier = Modifier
                                .size(48.dp)
                                .rotate(angle)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.labelSmall,
                            color = HeroOnDarkMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationX = dragOffsetX
                            // Subtle fade while dragged away, like a pager mid-scroll.
                            val w = size.width.coerceAtLeast(1f)
                            alpha = (1f - (abs(dragOffsetX) / w) * 0.35f).coerceIn(0.65f, 1f)
                        }
                        .pointerInput(state.quote?.id, state.isBrowsing, state.historyIndex) {
                            var totalX = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        totalX <= -swipeThresholdPx -> {
                                            swipeDirection = -1
                                            dragOffsetX = 0f
                                            viewModel.onEvent(HomeEvent.NextRandom)
                                        }
                                        totalX >= swipeThresholdPx && canGoPrev -> {
                                            swipeDirection = 1
                                            dragOffsetX = 0f
                                            viewModel.onEvent(HomeEvent.Prev)
                                        }
                                        else -> dragOffsetX = 0f
                                    }
                                    totalX = 0f
                                },
                                onDragCancel = {
                                    totalX = 0f
                                    dragOffsetX = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount
                                // Edge resistance at the history bottom, like an iOS pager bounce.
                                dragOffsetX = if (totalX > 0 && !canGoPrev) totalX * 0.35f else totalX
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(remember(state.quote?.id) { ScrollState(0) })
                    ) {
                        AnimatedContent(
                            targetState = state.quote?.id ?: state.error ?: "empty",
                            transitionSpec = {
                                // Direction-aware: next exits left / enters from right,
                                // prev mirrors it — standard pager semantics.
                                val dir = swipeDirection
                                (fadeIn(tween(200)) +
                                        slideInHorizontally(tween(200)) { dir * -it / 8 }) togetherWith
                                    (fadeOut(tween(200)) +
                                            slideOutHorizontally(tween(200)) { dir * it / 8 })
                            },
                            label = "quoteContent"
                        ) { key ->
                            val historySnapshot = state.history
                            val quoteSnapshot = state.quote
                            val errorSnapshot = state.error
                            val shown = historySnapshot.find { it.id == key } ?: quoteSnapshot
                            val quote = shown?.takeIf { it.id == key }
                            val error = if (quote == null) errorSnapshot else null
                            when {
                                quote != null && key == quote.id -> {
                                    HeroQuoteBlock(quote = quote)
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                error != null && key == error -> {
                                    HeroErrorBlock(
                                        message = error,
                                        onRetry = { viewModel.onEvent(HomeEvent.Load) },
                                        illustration = R.drawable.img_offline
                                    )
                                }
                                key == "empty" && !state.isLoading -> {
                                    when (state.emptyMode) {
                                        EmptyMode.NoCategoryLines -> HeroEmptyBlock(
                                            title = "No lines in this category yet",
                                            body = "This category hasn't been synced to your device.",
                                            actionLabel = "Switch to All",
                                            onAction = { viewModel.onEvent(HomeEvent.SelectCategory(null)) },
                                            illustration = R.drawable.empty_category
                                        )
                                        EmptyMode.Generic -> HeroEmptyBlock(
                                            title = "Nothing here yet",
                                            body = "Check your connection and try again.",
                                            actionLabel = "Retry",
                                            onAction = { viewModel.onEvent(HomeEvent.Load) },
                                            illustration = R.drawable.empty_quote
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTopBar(
    date: String,
    greeting: String,
    action: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = HeroOnDarkMuted
            )
            action()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayLarge,
            color = HeroOnDark
        )
    }
}

@Composable
private fun HeroQuoteBlock(quote: Quote, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = quote.content,
            style = MaterialTheme.typography.headlineLarge,
            color = HeroOnDark
        )
        if (!quote.translation.isNullOrBlank() || !quote.author.isNullOrBlank() || !quote.category.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = HeroOnDarkFaint, thickness = 1.dp)
        }
        if (!quote.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = quote.translation,
                style = MaterialTheme.typography.bodyLarge,
                color = HeroOnDarkMuted,
                fontStyle = FontStyle.Italic
            )
        }
        if (!quote.author.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "— ${quote.author}",
                style = MaterialTheme.typography.bodySmall,
                color = HeroOnDarkMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        if (!quote.category.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "#${quote.category.lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
            color = HeroOnDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = HeroOnDarkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = HeroOnDark,
            modifier = Modifier.clickable(onClick = onAction).padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun HeroErrorBlock(
    message: String,
    onRetry: () -> Unit,
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
            text = "Something went quiet",
            style = MaterialTheme.typography.headlineLarge,
            color = HeroOnDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = HeroOnDarkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Retry",
            style = MaterialTheme.typography.labelLarge,
            color = HeroOnDark,
            modifier = Modifier.clickable(onClick = onRetry).padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun HomeActionsBarOnDark(
    saveLabel: String,
    isSaved: Boolean,
    favoriteTapKey: Int,
    onSave: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = HeroOnDarkFaint, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroOutlineAction(
                label = saveLabel,
                onClick = onSave,
                isOn = isSaved,
                pulseKey = favoriteTapKey,
                modifier = Modifier.weight(1f)
            )
            HeroOutlineAction(
                label = "Next ->",
                onClick = onNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroOutlineAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOn: Boolean = false,
    pulseKey: Int = 0,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            scale.animateTo(0.96f, tween(80))
            scale.animateTo(1f, tween(160))
        }
    }
    val outline = if (isOn) HeroOnDark else HeroOnDarkFaint
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .background(Color.Transparent)
            .border(
                BorderStroke(1.dp, outline),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = HeroOnDark
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(
    selected: String?,
    available: List<String>,
    onPick: (String?) -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    // Filter-bar pattern (YouTube/Spotify): the row itself scrolls sideways and a
    // trailing fade hints there is more to slide out; overflow opens a bottom sheet.
    val chipsState = rememberLazyListState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("categoryChips")
    ) {
        LazyRow(
            state = chipsState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ChipLabel(text = "All", isSelected = selected == null, onClick = { onPick(null) })
            }
            items(available) { cat ->
                ChipLabel(text = cat, isSelected = selected == cat, onClick = { onPick(cat) })
            }
            if (available.size > 6 && onMore != null) {
                item {
                    ChipLabel(text = "More\u2026", isSelected = false, onClick = { sheetVisible = true })
                }
            }
        }
        if (chipsState.canScrollForward) {
            // NB: matchParentSize (not fillMaxHeight) so the fade stays row-high.
            // fillMaxHeight here would take the Column's full incoming height, stretch
            // this Box to full screen, and crush the weight(1f) quote zone to 0dp.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("chipsEdgeFade"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f)
                            )
                        )
                )
            }
        }
    }
    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                available.forEach { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onPick(cat)
                                sheetVisible = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChipLabel(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) HeroOnDark else HeroOnDarkMuted
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(HeroOnDark)
            )
        }
    }
}
