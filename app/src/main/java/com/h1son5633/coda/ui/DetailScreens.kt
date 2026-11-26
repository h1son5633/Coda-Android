package com.h1son5633.coda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.h1son5633.coda.net.Album
import com.h1son5633.coda.net.Artist
import com.h1son5633.coda.net.Playlist
import com.h1son5633.coda.net.Song
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "返回", Modifier.size(28.dp))
        }
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Centered hero shared by playlist/album pages (cover radius 12, actions 56 tall). */
@Composable
private fun DetailHero(
    coverUrl: String,
    title: String,
    creatorName: String,
    creatorAvatar: String,
    meta: String,
    onPlayAll: () -> Unit
) {
    var starred by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CodaTheme.pagePadding, vertical = 20.dp)
    ) {
        val coverSize = minOf(maxWidth * 0.68f, 300.dp)
        val actionRowWidth = minOf(maxWidth, 328.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .shadow(10.dp, RoundedCornerShape(CodaTheme.Radius.large))
            ) {
                CoverImage(coverUrl, coverSize, CodaTheme.Radius.large)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (creatorName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (creatorAvatar.isNotBlank()) {
                        AsyncImage(
                            model = creatorAvatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        creatorName,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                meta,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.width(actionRowWidth),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .clickable { starred = !starred },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (starred) CodaTheme.accent
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable { onPlayAll() }
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "播放",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.background
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = "更多",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InListSearch(
    placeholder: String,
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = CodaTheme.pagePadding)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(CodaTheme.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlaylistDetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val playerState by vm.player.state.collectAsState()
    val detail by produceState<Pair<Playlist, List<Song>>?>(null, id) {
        value = try { vm.api.playlistDetail(id) } catch (e: Exception) { null }
    }
    var filter by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        DetailTopBar(detail?.first?.name ?: "歌单", onBack)
        val data = detail
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CodaTheme.accent)
            }
            return@Column
        }
        val (playlist, tracks) = data
        val shown = if (filter.isBlank()) tracks else tracks.filter {
            it.name.contains(filter, true) || it.artists.contains(filter, true) ||
                it.album.contains(filter, true)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ChromeClearance)
        ) {
            item {
                InListSearch("在歌单中搜索", filter) { filter = it }
                if (filter.isNotBlank()) {
                    Text(
                        "找到 ${shown.size} 首匹配歌曲",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = CodaTheme.pagePadding, vertical = 8.dp
                        )
                    )
                }
                DetailHero(
                    coverUrl = playlist.cover(600),
                    title = playlist.name,
                    creatorName = playlist.creatorName,
                    creatorAvatar = "",
                    meta = "${playlist.trackCount} 首歌曲 · ${formatPlayCount(playlist.playCount)} 次播放",
                    onPlayAll = { if (tracks.isNotEmpty()) vm.player.playQueue(tracks, 0) }
                )
            }
            if (shown.isEmpty() && filter.isNotBlank()) {
                item { EmptyState("没有匹配的歌曲", "试试其他歌名、歌手或专辑") }
            }
            itemsIndexed(shown) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = playerState.current?.id == song.id,
                    isPlaying = playerState.isPlaying,
                    showAlbum = true,
                    onClick = { vm.player.playQueue(shown, index) }
                )
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val playerState by vm.player.state.collectAsState()
    val detail by produceState<Pair<Album, List<Song>>?>(null, id) {
        value = try { vm.api.albumDetail(id) } catch (e: Exception) { null }
    }
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(detail?.first?.name ?: "专辑", onBack)
        val data = detail
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CodaTheme.accent)
            }
            return@Column
        }
        val (album, songs) = data
        val totalMs = songs.sumOf { it.durationMs }
        val dateText = if (album.publishTime > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(album.publishTime))
        } else ""
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ChromeClearance)
        ) {
            item {
                DetailHero(
                    coverUrl = album.cover(600),
                    title = album.name,
                    creatorName = album.artistName,
                    creatorAvatar = "",
                    meta = listOf(
                        "${songs.size} 首歌曲",
                        formatDuration(totalMs),
                        dateText
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    onPlayAll = { if (songs.isNotEmpty()) vm.player.playQueue(songs, 0) }
                )
            }
            itemsIndexed(songs) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = playerState.current?.id == song.id,
                    isPlaying = playerState.isPlaying,
                    onClick = { vm.player.playQueue(songs, index) }
                )
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    val playerState by vm.player.state.collectAsState()
    val detail by produceState<Pair<Artist, List<Song>>?>(null, id) {
        value = try { vm.api.artistHotSongs(id) } catch (e: Exception) { null }
    }
    Column(Modifier.fillMaxSize()) {
        DetailTopBar(detail?.first?.name ?: "歌手", onBack)
        val data = detail
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CodaTheme.accent)
            }
            return@Column
        }
        val (artist, songs) = data
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ChromeClearance)
        ) {
            item {
                DetailHero(
                    coverUrl = artist.cover(600),
                    title = artist.name,
                    creatorName = "",
                    creatorAvatar = "",
                    meta = "${artist.musicSize} 首歌曲 · ${artist.albumSize} 张专辑",
                    onPlayAll = { if (songs.isNotEmpty()) vm.player.playQueue(songs, 0) }
                )
                SectionHeader(
                    "热门单曲",
                    modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                )
            }
            itemsIndexed(songs) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = playerState.current?.id == song.id,
                    isPlaying = playerState.isPlaying,
                    showAlbum = true,
                    onClick = { vm.player.playQueue(songs, index) }
                )
            }
            if (songs.isEmpty()) {
                item { EmptyState("暂无歌曲") }
            }
        }
    }
}

@Composable
fun DailySongsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val playerState by vm.player.state.collectAsState()
    val home by vm.home.collectAsState()
    val songs by produceState(home.dailySongs, home.dailySongs) {
        value = home.dailySongs.ifEmpty {
            try { vm.api.dailySongs() } catch (e: Exception) { emptyList() }
        }
    }
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("每日推荐", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ChromeClearance)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(
                        horizontal = CodaTheme.pagePadding, vertical = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(CodaTheme.Radius.large))
                            .background(CodaTheme.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = CodaTheme.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("每日推荐", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "根据你的音乐口味 · 每天 6:00 更新",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (songs.isEmpty()) {
                item { EmptyState("暂无推荐歌曲", "多听几首歌培养口味，每天 6:00 更新") }
            }
            itemsIndexed(songs) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = playerState.current?.id == song.id,
                    isPlaying = playerState.isPlaying,
                    showAlbum = true,
                    onClick = { vm.player.playQueue(songs, index) }
                )
            }
        }
    }
}

@Composable
fun ToplistsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenPlaylist: (Long) -> Unit
) {
    val home by vm.home.collectAsState()
    Column(Modifier.fillMaxSize()) {
        DetailTopBar("排行榜", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                top = 8.dp, bottom = ChromeClearance
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(home.toplists) { chart ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CodaTheme.Radius.large))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .clickable { onOpenPlaylist(chart.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverImage(chart.cover(300), 110.dp, CodaTheme.Radius.standard)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            chart.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            chart.updateFrequency.ifBlank { "${chart.trackCount} 首歌曲" },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (chart.updateFrequency.isNotBlank()) {
                            Text(
                                "${chart.trackCount} 首歌曲 · ${formatPlayCount(chart.playCount)} 次播放",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (home.toplists.isEmpty()) {
                item { EmptyState("暂无榜单") }
            }
        }
    }
}
