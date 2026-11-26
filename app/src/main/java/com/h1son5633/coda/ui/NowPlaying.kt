package com.h1son5633.coda.ui

import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.h1son5633.coda.data.LyricLine
import com.h1son5633.coda.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

private enum class NPPanel { NONE, QUEUE }

/**
 * Now Playing, portrait phone form, following the original's
 * NowPlayingView/ControlMetrics: fluid cover-color background, a lyrics
 * state (the original's default) and a cover state sharing one control
 * stack (progress 52 / gap 24 / controls 82 / gap 36 / volume 32 / gap 4 /
 * toolbar 50, track height 7, scaled by width/356 clamped 0.95–1.15).
 */
@Composable
fun NowPlayingScreen(vm: AppViewModel, onClose: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val lyrics by vm.lyrics.collectAsState()
    val song = state.current
    var lyricsMode by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(NPPanel.NONE) }
    var liked by remember(song?.id) { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }

    // Lyrics state: controls auto-hide after 3s while playing.
    LaunchedEffect(state.isPlaying, lyricsMode, panel, interactionTick) {
        if (lyricsMode && panel == NPPanel.NONE && state.isPlaying) {
            controlsVisible = true
            delay(3000)
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    val palette = rememberArtworkPalette(song?.cover(400) ?: "")

    MaterialTheme(colorScheme = darkColorScheme(primary = CodaTheme.accent)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF101014))) {
            // Fluid background approximation: blurred cover over palette gradient.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(palette.primary, palette.secondary)
                        )
                    )
            )
            if (!song?.picUrl.isNullOrBlank()) {
                AsyncImage(
                    model = song!!.cover(400),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= 31) Modifier.blur(70.dp)
                            else Modifier
                        )
                        .graphicsLayer { scaleX = 1.6f; scaleY = 1.6f; alpha = 0.85f }
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.48f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                // Dismiss handle (60×5, white 0.52).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable { onClose() },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(width = 60.dp, height = 5.dp)
                            .background(Color.White.copy(alpha = 0.52f), CircleShape)
                    )
                }

                when {
                    panel == NPPanel.QUEUE -> {
                        QueuePanel(
                            vm,
                            modifier = Modifier.weight(1f),
                            onPick = { interactionTick++ }
                        )
                    }
                    lyricsMode -> {
                        // Small header row (72 cover) + lyrics filling the middle.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoverImage(song?.cover(200) ?: "", 72.dp, CodaTheme.Radius.large)
                            Spacer(Modifier.width(12.dp))
                            TrackInfo(
                                title = song?.name ?: "",
                                artist = song?.artists ?: "",
                                liked = liked,
                                onToggleLike = { liked = !liked },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LyricsColumn(
                            lines = lyrics,
                            positionMs = state.positionMs,
                            onSeek = { vm.player.seekTo(it) },
                            onTapArea = {
                                controlsVisible = !controlsVisible
                                interactionTick++
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        // Cover state.
                        val scale by animateFloatAsState(
                            targetValue = if (state.isPlaying) 1f else 0.74f,
                            label = "coverScale"
                        )
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val coverSize = min(maxWidth.value, 380f).dp
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(coverSize)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .shadow(
                                            if (state.isPlaying) 26.dp else 14.dp,
                                            RoundedCornerShape(CodaTheme.Radius.large)
                                        )
                                ) {
                                    CoverImage(
                                        song?.cover(800) ?: "",
                                        coverSize,
                                        CodaTheme.Radius.large
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        TrackInfo(
                            title = song?.name ?: "",
                            artist = song?.artists ?: "",
                            liked = liked,
                            onToggleLike = { liked = !liked }
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }

                AnimatedVisibility(
                    visible = controlsVisible || !lyricsMode || panel != NPPanel.NONE,
                    enter = fadeIn() + slideInVertically { it / 3 },
                    exit = fadeOut() + slideOutVertically { it / 3 }
                ) {
                    ControlStack(
                        vm,
                        showFull = panel == NPPanel.NONE,
                        lyricsMode = lyricsMode,
                        queueActive = panel == NPPanel.QUEUE,
                        onToggleLyrics = {
                            lyricsMode = !lyricsMode
                            panel = NPPanel.NONE
                            interactionTick++
                        },
                        onToggleQueue = {
                            panel = if (panel == NPPanel.QUEUE) NPPanel.NONE else NPPanel.QUEUE
                            interactionTick++
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackInfo(
    title: String,
    artist: String,
    liked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                artist,
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { onToggleLike() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "喜欢",
                tint = if (liked) CodaTheme.heartRed else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ControlStack(
    vm: AppViewModel,
    showFull: Boolean,
    lyricsMode: Boolean,
    queueActive: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit
) {
    val state by vm.player.state.collectAsState()
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }
    var volume by remember {
        mutableStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        )
    }

    Column {
        if (showFull) {
            // Progress row (52 tall, track 7).
            ProgressBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = { vm.player.seekTo(it) }
            )
            Spacer(Modifier.height(24.dp))
            // Playback controls (82 tall, five equal cells).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlCell(
                    active = state.shuffle,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.player.toggleShuffle() }
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "随机",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                ControlCell(modifier = Modifier.weight(1f), onClick = { vm.player.previous() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                ControlCell(modifier = Modifier.weight(1f), onClick = { vm.player.toggle() }) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "播放/暂停",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                ControlCell(modifier = Modifier.weight(1f), onClick = { vm.player.next() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                ControlCell(
                    active = state.repeatMode != RepeatMode.OFF,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.player.cycleRepeat() }
                ) {
                    Icon(
                        if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne
                        else Icons.Filled.Repeat,
                        contentDescription = "循环",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            // Volume row (32 tall).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.VolumeDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = { v ->
                        volume = v
                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC, (v * max).toInt(), 0
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.6f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        // Toolbar (50 tall): device on the left, panel toggles on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "本机",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
            Spacer(Modifier.weight(1f))
            ToolbarButton(
                active = queueActive,
                onClick = onToggleQueue
            ) {
                Icon(
                    Icons.Filled.QueueMusic,
                    contentDescription = "待播清单",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            ToolbarButton(
                active = lyricsMode,
                onClick = onToggleLyrics
            ) {
                Icon(
                    Icons.Filled.FormatQuote,
                    contentDescription = "歌词",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlCell(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun ToolbarButton(
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                .padding(9.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    if (active) Color.White else Color.White.copy(alpha = 0.65f)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableStateOf(0) }
    val fraction = dragFraction
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val displayMs = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (widthPx > 0 && durationMs > 0) {
                            onSeek((offset.x / widthPx * durationMs).toLong())
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (widthPx > 0) {
                                dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                            }
                        },
                        onDrag = { change, _ ->
                            if (widthPx > 0) {
                                dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            if (dragFraction != null) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        Modifier
                            .size(13.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                formatDuration(displayMs),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                "−" + formatDuration((durationMs - displayMs).coerceAtLeast(0)),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun LyricsColumn(
    lines: List<LyricLine>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onTapArea: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.FormatQuote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("暂无歌词", fontSize = 15.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
        return
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var viewportHeight by remember { mutableStateOf(0) }
    val activeIndex = remember(lines, positionMs) {
        val idx = lines.indexOfLast { it.startMs <= positionMs }
        idx
    }
    LaunchedEffect(activeIndex, viewportHeight) {
        if (activeIndex >= 0 && viewportHeight > 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(
                activeIndex,
                scrollOffset = -(viewportHeight * 0.36f).toInt()
            )
        }
    }
    val topPad = with(density) { (viewportHeight * 0.36f).toDp() }
    val bottomPad = with(density) { (viewportHeight * 0.5f).toDp() }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { viewportHeight = it.height }
            .pointerInput(Unit) {
                detectTapGestures { onTapArea() }
            },
        contentPadding = PaddingValues(top = topPad, bottom = bottomPad)
    ) {
        itemsIndexed(lines) { index, line ->
            LyricLineView(
                line = line,
                isActive = index == activeIndex,
                distance = if (activeIndex < 0) index else abs(index - activeIndex),
                sung = activeIndex >= 0 && index < activeIndex,
                positionMs = positionMs,
                onClick = { onSeek(line.startMs) }
            )
        }
    }
}

@Composable
private fun LyricLineView(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    sung: Boolean,
    positionMs: Long,
    onClick: () -> Unit
) {
    val alpha = when {
        isActive -> 1f
        sung -> 0.30f
        else -> maxOf(0.30f, 0.58f - 0.055f * distance)
    }
    val blurRadius = when {
        isActive || Build.VERSION.SDK_INT < 31 -> 0.dp
        else -> min(3.5f, 0.65f * distance).dp
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        if (isActive && line.words.isNotEmpty()) {
            // Word-by-word (karaoke) highlight: sung 1.0, pending floor 0.28.
            val annotated = buildAnnotatedString {
                for (word in line.words) {
                    val wordAlpha = when {
                        positionMs >= word.startMs + word.durationMs -> 1f
                        positionMs <= word.startMs -> 0.28f
                        word.durationMs <= 0 -> 1f
                        else -> 0.28f + 0.72f *
                            ((positionMs - word.startMs).toFloat() / word.durationMs)
                    }
                    withStyle(SpanStyle(color = Color.White.copy(alpha = wordAlpha))) {
                        append(word.text)
                    }
                }
            }
            Text(
                annotated,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp
            )
        } else {
            Text(
                line.text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp,
                color = Color.White.copy(alpha = alpha),
                modifier = if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier
            )
        }
        if (line.translation.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                line.translation,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = if (isActive) 0.76f else 0.25f)
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun QueuePanel(vm: AppViewModel, modifier: Modifier = Modifier, onPick: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val queue = remember(state.queueIndex, state.queueSize, state.current) {
        vm.player.queueSnapshot()
    }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "待播清单",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${queue.size} 首歌曲",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(queue) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = index == state.queueIndex,
                    isPlaying = state.isPlaying,
                    showAlbum = true,
                    onClick = {
                        vm.player.jumpTo(index)
                        onPick()
                    }
                )
            }
        }
    }
}
