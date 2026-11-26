package com.h1son5633.coda.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Bottom space pages must reserve for the floating chrome (mini player + dock). */
val ChromeClearance = 170.dp

@Composable
fun Shell(vm: AppViewModel) {
    var tab by remember { mutableStateOf(0) }
    val stacks = remember {
        listOf(
            mutableStateListOf<Dest>(),
            mutableStateListOf<Dest>(),
            mutableStateListOf<Dest>()
        )
    }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var loginOpen by remember { mutableStateOf(false) }
    val playerState by vm.player.state.collectAsState()
    val loginState by vm.login.collectAsState()

    fun push(dest: Dest) {
        stacks[tab].add(dest)
    }

    fun openLogin() {
        loginOpen = true
    }

    BackHandler(enabled = nowPlayingOpen) { nowPlayingOpen = false }
    BackHandler(enabled = !nowPlayingOpen && stacks[tab].isNotEmpty()) {
        stacks[tab].removeAt(stacks[tab].lastIndex)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val currentDest = stacks[tab].lastOrNull()
        // Pages sit below the (transparent) status bar; the app background
        // paints behind it, FlClash-style edge-to-edge.
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
        when {
            currentDest is Dest.PlaylistDetail -> PlaylistDetailScreen(
                vm, currentDest.id,
                onBack = { stacks[tab].removeAt(stacks[tab].lastIndex) }
            )
            currentDest is Dest.AlbumDetail -> AlbumDetailScreen(
                vm, currentDest.id,
                onBack = { stacks[tab].removeAt(stacks[tab].lastIndex) }
            )
            currentDest is Dest.ArtistDetail -> ArtistDetailScreen(
                vm, currentDest.id,
                onBack = { stacks[tab].removeAt(stacks[tab].lastIndex) }
            )
            currentDest is Dest.DailySongs -> DailySongsScreen(
                vm,
                onBack = { stacks[tab].removeAt(stacks[tab].lastIndex) }
            )
            currentDest is Dest.Toplists -> ToplistsScreen(
                vm,
                onBack = { stacks[tab].removeAt(stacks[tab].lastIndex) },
                onOpenPlaylist = { push(Dest.PlaylistDetail(it)) }
            )
            tab == 0 -> HomeScreen(
                vm,
                onOpenPlaylist = { push(Dest.PlaylistDetail(it)) },
                onOpenAlbum = { push(Dest.AlbumDetail(it)) },
                onOpenDaily = { push(Dest.DailySongs) },
                onOpenToplists = { push(Dest.Toplists) },
                onLoginRequest = { openLogin() }
            )
            tab == 1 -> ExploreScreen(
                vm,
                onOpenPlaylist = { push(Dest.PlaylistDetail(it)) },
                onOpenAlbum = { push(Dest.AlbumDetail(it)) },
                onOpenArtist = { push(Dest.ArtistDetail(it)) }
            )
            else -> LibraryScreen(
                vm,
                onOpenPlaylist = { push(Dest.PlaylistDetail(it)) },
                onLoginRequest = { openLogin() }
            )
        }
        }

        // Floating chrome: mini player capsule over the FlClash-style dock.
        if (!nowPlayingOpen) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniPlayer(
                    vm,
                    onOpen = { if (playerState.current != null) nowPlayingOpen = true },
                    modifier = Modifier.padding(horizontal = 21.dp)
                )
                DockTabBar(
                    selected = tab,
                    onSelect = { newTab ->
                        if (newTab == tab) {
                            stacks[tab].clear()
                        } else {
                            tab = newTab
                        }
                    }
                )
            }
        }

        // Gated-song notice (the original surfaces playback failures as a top toast).
        AnimatedVisibility(
            visible = playerState.unavailableSong != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val song = playerState.unavailableSong
            LaunchedEffect(song) {
                delay(4000)
                vm.player.dismissUnavailable()
            }
            Row(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "「${song?.name ?: ""}」需要登录或会员才能播放",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (loginState.profile == null) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "登录",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CodaTheme.accent,
                        modifier = Modifier.clickable {
                            vm.player.dismissUnavailable()
                            openLogin()
                        }
                    )
                }
            }
        }

        // Now Playing, full-screen over everything.
        AnimatedVisibility(
            visible = nowPlayingOpen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            NowPlayingScreen(vm, onClose = { nowPlayingOpen = false })
        }

        if (loginOpen) {
            LoginSheet(
                vm,
                onDismiss = { loginOpen = false },
                onSuccess = { loginOpen = false }
            )
        }
    }
}

private data class TabItem(val title: String, val icon: ImageVector)

// ---- FlClash NavigationDock port -------------------------------------------
// A faithful Compose port of chen08209/prism_ui's NavigationDock (itself
// measured from iOS's floating tab bar): a capsule floating 21dp from the
// sides and max(21dp, nav-bar inset) off the bottom, at most 86dp per
// destination wide; a selection lens on spring physics that the finger can
// press, lift, drag and fling; velocity jelly on the lens; a press swell on
// the whole bar; content turns accent-colored only where the lens covers it.

private fun dockRubberBand(overshoot: Float, limit: Float): Float {
    val pull = 1f - 1f / (kotlin.math.abs(overshoot) * 0.55f / limit + 1f)
    return limit * pull * kotlin.math.sign(overshoot)
}

private fun dockSaturate(value: Float): Float =
    kotlin.math.tanh(value.toDouble()).toFloat()

private val dockTravelSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 247f)
private val dockFollowSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 1024f)
private val dockPressSpring = spring<Float>(dampingRatio = 0.593f, stiffness = 311f)
private val dockLiftSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 630f)
private val dockJellySpring = spring<Float>(dampingRatio = 0.4f, stiffness = 100f)
private val dockTrackSpring = spring<Float>(dampingRatio = 1.0f, stiffness = 2700f)

@Composable
private fun DockTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        TabItem("推荐", Icons.Filled.Home),
        TabItem("探索", Icons.Filled.Search),
        TabItem("我的", Icons.Filled.AccountCircle)
    )
    val dark = MaterialTheme.colorScheme.background == Color(0xFF101012)
    val density = LocalDensity.current
    val navInset = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val bottomMargin = maxOf(21.dp, navInset)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val lens = remember { Animatable(selected.toFloat()) }
    val lift = remember { Animatable(0f) }
    val swell = remember { Animatable(0f) }
    val jelly = remember { Animatable(0f) }
    val stretch = remember { Animatable(0f) }
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var lensTarget by remember { mutableStateOf(selected.toFloat()) }
    var liftDropJob by remember { mutableStateOf<Job?>(null) }

    // Selection changed from outside the dock (not by a press): follow it.
    LaunchedEffect(selected) {
        if (pressedIndex == null) {
            lensTarget = selected.toFloat()
            lens.animateTo(selected.toFloat(), dockTravelSpring)
        }
    }

    // Jelly: the lens stretches with its speed and squashes while braking.
    LaunchedEffect(Unit) {
        var prevVelocity = 0f
        var prevMillis = 0L
        while (true) {
            withFrameMillis { millis ->
                val velocity = lens.velocity
                if (prevMillis != 0L) {
                    val dt = (millis - prevMillis) / 1000f
                    if (dt > 0.001f) {
                        val acceleration = (velocity - prevVelocity) / dt
                        val braking = max(
                            0f, -acceleration * kotlin.math.sign(velocity)
                        )
                        val target = 0.2f * dockSaturate(
                            kotlin.math.abs(velocity) / 3f
                        ) - 0.15f * dockSaturate(braking / 25f)
                        if (kotlin.math.abs(target - jelly.targetValue) > 0.002f) {
                            scope.launch { jelly.animateTo(target, dockJellySpring) }
                        }
                    }
                }
                prevVelocity = velocity
                prevMillis = millis
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 21.dp)
            .padding(bottom = bottomMargin)
            .padding(top = 8.dp)
    ) {
        val count = items.size
        val barWidth = minOf(maxWidth, 274.dp)
        val pad = 4.dp
        val span = barWidth - pad * 2
        // Even layout for <=3 destinations, as in the original: each lens is
        // a touch wider than its cell, neighbours overlapping by 8.
        val lensWidth = (span + 16.dp) / count
        val cellWidth = lensWidth - 8.dp
        val inset = 4.dp
        fun centerAt(position: Float) =
            pad + inset + cellWidth * (position + 0.5f)
        fun positionOf(x: Dp): Float {
            val raw = ((x - pad - inset) / cellWidth) - 0.5f
            return when {
                raw < 0f -> dockRubberBand(raw, 0.35f)
                raw > (count - 1) -> (count - 1) +
                    dockRubberBand(raw - (count - 1), 0.35f)
                else -> raw
            }
        }
        fun indexAt(position: Float): Int =
            position.roundToInt().coerceIn(0, count - 1)

        val liftNow = lift.value
        val jellyNow = jelly.value
        val lensW = (lensWidth + 16.dp * liftNow) * (1f + jellyNow)
        val lensH = (54.dp + 16.dp * liftNow) * (1f - jellyNow * 1.4f)
        val lensLeft = centerAt(lens.value) - lensW / 2
        val lensTop = pad + (54.dp - lensH) / 2

        val barColor = if (dark) Color(0xFF26262B) else Color.White
        val lensBase = if (dark) Color(0xFF34343A) else Color(0xFFEEEEEF)
        val onSurface = MaterialTheme.colorScheme.onSurface
        val wash = 0.08f * liftNow
        val lensColor = Color(
            red = onSurface.red * wash + lensBase.red * (1f - wash),
            green = onSurface.green * wash + lensBase.green * (1f - wash),
            blue = onSurface.blue * wash + lensBase.blue * (1f - wash)
        )
        val baseTint = MaterialTheme.colorScheme.onSurfaceVariant

        val itemsRow: @Composable (Color) -> Unit = { color ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = pad + inset, vertical = pad)
            ) {
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = color
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            item.title,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = color,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }

        val swellScale = 1f + swell.value * min(0.25f, 16f / barWidth.value)
        val pullDp = stretch.value
        val barScaleX = swellScale *
            (1f + kotlin.math.abs(pullDp) / barWidth.value * 0.5f)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Hit area: the bar plus an 8dp slop halo, as in the original.
            Box(
                modifier = Modifier
                    .width(barWidth + 16.dp)
                    .height(78.dp)
                    .pointerInput(Unit) {
                        val tracker = VelocityTracker()
                        awaitPointerEventScope {
                            while (true) {
                                val downEvent = awaitPointerEvent()
                                val change = downEvent.changes
                                    .firstOrNull { it.changedToDown() } ?: continue
                                tracker.resetTracking()
                                tracker.addPosition(
                                    change.uptimeMillis, change.position
                                )
                                val startX =
                                    (change.position.x / density.density).dp - 8.dp
                                var dragging = false
                                var dragIndex = indexAt(positionOf(startX))
                                pressedIndex = dragIndex
                                liftDropJob?.cancel()
                                scope.launch { swell.animateTo(1f, dockPressSpring) }
                                scope.launch { lift.animateTo(1f, dockLiftSpring) }
                                lensTarget = dragIndex.toFloat()
                                scope.launch {
                                    lens.animateTo(
                                        dragIndex.toFloat(), dockTravelSpring
                                    )
                                }
                                var released = false
                                var cancelled = false
                                while (!released) {
                                    val event = awaitPointerEvent()
                                    val c = event.changes
                                        .firstOrNull { it.id == change.id } ?: break
                                    when {
                                        c.isCanceled -> {
                                            released = true
                                            cancelled = true
                                        }
                                        c.changedToUp() -> released = true
                                        c.positionChanged() -> {
                                            tracker.addPosition(
                                                c.uptimeMillis, c.position
                                            )
                                            val x = (c.position.x /
                                                density.density).dp - 8.dp
                                            if (!dragging &&
                                                kotlin.math.abs((x - startX).value) > 18f
                                            ) {
                                                dragging = true
                                            }
                                            if (dragging) {
                                                val over = when {
                                                    x < 0.dp -> x.value
                                                    x > barWidth -> (x - barWidth).value
                                                    else -> 0f
                                                }
                                                scope.launch {
                                                    stretch.animateTo(
                                                        dockRubberBand(
                                                            over, 62f * 7f / 32f
                                                        ),
                                                        dockTrackSpring
                                                    )
                                                }
                                                val pos = positionOf(x)
                                                lensTarget = pos
                                                scope.launch {
                                                    lens.animateTo(
                                                        pos, dockFollowSpring
                                                    )
                                                }
                                                val idx = indexAt(pos)
                                                if (idx != dragIndex) {
                                                    dragIndex = idx
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.TextHandleMove
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                scope.launch { swell.animateTo(0f, dockPressSpring) }
                                scope.launch {
                                    stretch.animateTo(0f, dockPressSpring)
                                }
                                var commitIndex = dragIndex
                                if (!cancelled && dragging) {
                                    val velocity = tracker.calculateVelocity().x /
                                        density.density
                                    val lead = velocity / cellWidth.value * 0.1f
                                    val flung = (lensTarget + lead).roundToInt()
                                        .coerceIn(dragIndex - 1, dragIndex + 1)
                                        .coerceIn(0, count - 1)
                                    if (flung != dragIndex) {
                                        commitIndex = flung
                                        haptics.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                    }
                                }
                                val targetIndex =
                                    if (cancelled) selected else commitIndex
                                lensTarget = targetIndex.toFloat()
                                scope.launch {
                                    lens.animateTo(
                                        targetIndex.toFloat(), dockTravelSpring
                                    )
                                }
                                pressedIndex = null
                                if (!cancelled && commitIndex != selected) {
                                    onSelect(commitIndex)
                                }
                                // The lens stays lifted until it has fully
                                // risen and arrived, then falls.
                                val arrived = targetIndex.toFloat()
                                liftDropJob = scope.launch {
                                    snapshotFlow {
                                        lift.value >= 0.975f &&
                                            kotlin.math.abs(lens.value - arrived) <= 0.04f
                                    }.first { it }
                                    lift.animateTo(0f, dockLiftSpring)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(62.dp)
                        .graphicsLayer {
                            scaleX = barScaleX
                            scaleY = swellScale
                            translationX = pullDp * density.density
                        }
                        .shadow(
                            12.dp, CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.16f),
                            spotColor = Color.Black.copy(alpha = 0.16f)
                        )
                        .clip(CircleShape)
                        .background(barColor)
                        .border(
                            1.dp,
                            (if (dark) Color.White else Color.Black)
                                .copy(alpha = 0.06f),
                            CircleShape
                        )
                ) {
                    // The lens itself.
                    Box(
                        modifier = Modifier
                            .offset(x = lensLeft, y = lensTop)
                            .size(lensW, lensH)
                            .clip(CircleShape)
                            .background(lensColor)
                    )
                    itemsRow(baseTint)
                    // Accent copy of the items, visible only through the
                    // lens and magnified as the lens lifts (_LensTint).
                    Box(
                        modifier = Modifier
                            .offset(x = lensLeft, y = lensTop)
                            .size(lensW, lensH)
                            .clip(CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = -lensLeft, y = -lensTop)
                                .size(barWidth, 62.dp)
                                .graphicsLayer {
                                    scaleX = 1f + 0.16f * liftNow
                                    scaleY = 1f + 0.16f * liftNow
                                    transformOrigin = TransformOrigin(
                                        (centerAt(lens.value) / barWidth)
                                            .coerceIn(0f, 1f),
                                        0.5f
                                    )
                                }
                        ) {
                            itemsRow(CodaTheme.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    vm: AppViewModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.player.state.collectAsState()
    val song = state.current
    val dark = MaterialTheme.colorScheme.background == Color(0xFF101012)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                1.dp,
                (if (dark) Color.White else Color.Black).copy(alpha = 0.08f),
                CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = song != null) { onOpen() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverImage(song?.cover(200) ?: "", 40.dp, CodaTheme.Radius.small)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    song?.name ?: "未在播放",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    song?.artists ?: "Coda",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        MiniControl(
            icon = Icons.Filled.SkipPrevious,
            enabled = song != null,
            onClick = { vm.player.previous() }
        )
        if (state.isBuffering) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            MiniControl(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                enabled = song != null,
                onClick = { vm.player.toggle() }
            )
        }
        MiniControl(
            icon = Icons.Filled.SkipNext,
            enabled = song != null,
            onClick = { vm.player.next() }
        )
    }
}

@Composable
private fun MiniControl(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 1f else 0.35f
            )
        )
    }
}
