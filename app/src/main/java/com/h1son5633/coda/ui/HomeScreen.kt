package com.h1son5633.coda.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenToplists: () -> Unit,
    onLoginRequest: () -> Unit
) {
    val home by vm.home.collectAsState()
    val login by vm.login.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val loggedIn = login.profile != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ChromeClearance)
    ) {
        item {
            Text(
                "推荐",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = CodaTheme.pagePadding, end = CodaTheme.pagePadding,
                    top = 16.dp, bottom = 8.dp
                )
            )
        }

        if (!loggedIn) {
            item {
                HomeLoginPrompt(onLoginRequest)
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        } else if (home.dailySongs.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DailyRecommendCard(
                            coverUrl = home.dailySongs.first().cover(400),
                            onClick = onOpenDaily
                        )
                    }
                }
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        }

        // 排行榜: chart columns with inline top-10 previews.
        if (home.toplists.isNotEmpty()) {
            item {
                SectionHeader(
                    "排行榜",
                    modifier = Modifier.padding(horizontal = CodaTheme.pagePadding),
                    onClick = onOpenToplists
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(home.toplists.take(6)) { chart ->
                        Column(Modifier.width(357.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clickable { onOpenPlaylist(chart.id) }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverImage(chart.cover(200), 48.dp, CodaTheme.Radius.small)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        chart.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (chart.updateFrequency.isNotBlank()) {
                                        Text(
                                            chart.updateFrequency,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Divider(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                            val preview = home.toplistPreviews[chart.id] ?: emptyList()
                            preview.forEachIndexed { index, song ->
                                SongRow(
                                    song = song,
                                    isCurrent = playerState.current?.id == song.id,
                                    isPlaying = playerState.isPlaying,
                                    showAlbum = true,
                                    rank = index + 1,
                                    onClick = { vm.player.playQueue(preview, index) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        }

        // 推荐歌单: double-row cover shelf.
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
                                PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.id) })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        }

        // 新碟上架: single-row album shelf.
        if (home.newAlbums.isNotEmpty()) {
            item {
                SectionHeader(
                    "新碟上架",
                    modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(home.newAlbums) { album ->
                        AlbumCard(album, onClick = { onOpenAlbum(album.id) })
                    }
                }
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        }

        if (home.loading && home.personalized.isEmpty() && home.toplists.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CodaTheme.pagePadding)
                        .height(178.dp)
                        .clip(RoundedCornerShape(CodaTheme.Radius.cover))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun HomeLoginPrompt(onLoginRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CodaTheme.pagePadding)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("登录网易云后查看", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "每日推荐、私人推荐等内容需登录后查看",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onLoginRequest,
            colors = ButtonDefaults.buttonColors(containerColor = CodaTheme.accent)
        ) {
            Text("登录网易云", color = Color.White)
        }
    }
}

/** The daily-recommend feature card: first song's cover under a bottom gradient. */
@Composable
private fun DailyRecommendCard(coverUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(178.dp)
            .clip(RoundedCornerShape(CodaTheme.Radius.cover))
            .background(Color(0xFF474747))
            .clickable { onClick() }
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "每日推荐",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                "根据你的口味每天更新",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
