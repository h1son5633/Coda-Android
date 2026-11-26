package com.h1son5633.coda.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Bottom space pages must reserve for the floating chrome (mini player + tab bar). */
val ChromeClearance = 142.dp

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

        // Floating chrome: mini player capsule over the glass tab bar.
        if (!nowPlayingOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniPlayer(
                    vm,
                    onOpen = { if (playerState.current != null) nowPlayingOpen = true }
                )
                GlassTabBar(
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

@Composable
private fun GlassTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        TabItem("推荐", Icons.Filled.Home),
        TabItem("探索", Icons.Filled.Search),
        TabItem("我的", Icons.Filled.AccountCircle)
    )
    val dark = MaterialTheme.colorScheme.background == Color(0xFF101012)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            )
            .border(
                1.dp,
                (if (dark) Color.White else Color.Black).copy(alpha = 0.08f),
                CircleShape
            )
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            (if (dark) Color.White else Color.Black).copy(
                                alpha = if (dark) 0.10f else 0.075f
                            )
                        } else Color.Transparent
                    )
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(23.dp),
                    tint = if (isSelected) CodaTheme.accent
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) CodaTheme.accent
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(vm: AppViewModel, onOpen: () -> Unit) {
    val state by vm.player.state.collectAsState()
    val song = state.current
    val dark = MaterialTheme.colorScheme.background == Color(0xFF101012)
    Row(
        modifier = Modifier
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
