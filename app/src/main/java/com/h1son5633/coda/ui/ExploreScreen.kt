package com.h1son5633.coda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.h1son5633.coda.net.Album
import com.h1son5633.coda.net.Artist
import com.h1son5633.coda.net.Playlist

private val Segments = listOf("综合", "单曲", "歌手", "专辑", "歌单")

@Composable
fun ExploreScreen(
    vm: AppViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit
) {
    val search by vm.search.collectAsState()
    val home by vm.home.collectAsState()
    val playerState by vm.player.state.collectAsState()
    var segment by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

    val query = search.query
    val trimmed = query.trim()
    val extras by produceState(
        initialValue = Triple(emptyList<Artist>(), emptyList<Album>(), emptyList<Playlist>()),
        trimmed
    ) {
        if (trimmed.isBlank()) return@produceState
        val artists = try { vm.api.searchArtists(trimmed) } catch (e: Exception) { emptyList() }
        val albums = try { vm.api.searchAlbums(trimmed) } catch (e: Exception) { emptyList() }
        val playlists = try { vm.api.searchPlaylists(trimmed) } catch (e: Exception) { emptyList() }
        value = Triple(artists, albums, playlists)
    }
    val hotKeywords by produceState(initialValue = emptyList<String>(), Unit) {
        value = try { vm.api.hotKeywords() } catch (e: Exception) { emptyList() }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            if (trimmed.isBlank()) "探索" else "搜索：$trimmed",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                top = 16.dp, bottom = 8.dp
            )
        )

        // In-page search field (original: rounded 12, 48 tall, page inset 20).
        Row(
            modifier = Modifier
                .padding(horizontal = CodaTheme.pagePadding)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(CodaTheme.Radius.large))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .padding(start = 16.dp),
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
                        "搜索歌曲、歌手、专辑、歌单",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { vm.onQueryChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(CodaTheme.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        vm.submitSearch()
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { vm.onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (trimmed.isBlank()) {
            // Hot keywords + recommended shelves (empty state).
            if (hotKeywords.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(hotKeywords.take(12)) { word ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .clickable {
                                    vm.onQueryChange(word)
                                    vm.submitSearch()
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(word, fontSize = 15.sp)
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 24.dp, bottom = ChromeClearance
                )
            ) {
                if (home.personalized.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "推荐歌单",
                            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                        )
                        Spacer(Modifier.height(16.dp))
                        val columns = home.personalized.chunked(2)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(columns) { column ->
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    column.forEach { playlist ->
                                        PlaylistCard(
                                            playlist,
                                            onClick = { onOpenPlaylist(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // Segment switcher.
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Segments.forEachIndexed { index, label ->
                val selected = segment == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) CodaTheme.accent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        .clickable { segment = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) androidx.compose.ui.graphics.Color.White
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when (segment) {
            1 -> SongResults(vm, onLoadMore = { vm.loadMoreSongs() })
            2 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                    top = 8.dp, bottom = ChromeClearance
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(extras.first) { artist ->
                    ArtistCircleCard(artist, onClick = { onOpenArtist(artist.id) })
                }
            }
            3 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                    top = 8.dp, bottom = ChromeClearance
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(extras.second) { album ->
                    AlbumCard(album, onClick = { onOpenAlbum(album.id) })
                }
            }
            4 -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                    top = 8.dp, bottom = ChromeClearance
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(extras.third) { playlist ->
                    PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.id) })
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ChromeClearance)
            ) {
                if (search.results.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "单曲",
                            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
                            onClick = { segment = 1 }
                        )
                    }
                    itemsIndexed(search.results.take(8)) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = playerState.current?.id == song.id,
                            isPlaying = playerState.isPlaying,
                            onClick = { vm.player.playQueue(search.results, index) }
                        )
                    }
                }
                if (extras.first.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "歌手",
                            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
                            onClick = { segment = 2 }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(extras.first) { artist ->
                                ArtistCircleCard(artist, onClick = { onOpenArtist(artist.id) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (extras.second.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "专辑",
                            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
                            onClick = { segment = 3 }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(extras.second) { album ->
                                AlbumCard(album, onClick = { onOpenAlbum(album.id) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (extras.third.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "歌单",
                            modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
                            onClick = { segment = 4 }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(extras.third) { playlist ->
                                PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.id) })
                            }
                        }
                    }
                }
                if (search.searched && !search.loading && search.results.isEmpty() &&
                    extras.first.isEmpty() && extras.second.isEmpty() && extras.third.isEmpty()
                ) {
                    item { EmptyState("没有找到相关结果") }
                }
                if (search.loading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CodaTheme.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongResults(vm: AppViewModel, onLoadMore: () -> Unit) {
    val search by vm.search.collectAsState()
    val playerState by vm.player.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ChromeClearance)
    ) {
        itemsIndexed(search.results) { index, song ->
            if (index >= search.results.size - 5) {
                LaunchedEffect(index) { onLoadMore() }
            }
            SongRow(
                song = song,
                isCurrent = playerState.current?.id == song.id,
                isPlaying = playerState.isPlaying,
                onClick = { vm.player.playQueue(search.results, index) }
            )
        }
        if (search.searched && search.results.isEmpty() && !search.loading) {
            item { EmptyState("没有找到相关结果") }
        }
        if (search.loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CodaTheme.accent)
                }
            }
        }
    }
}
