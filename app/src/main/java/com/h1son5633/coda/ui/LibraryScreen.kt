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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun LibraryScreen(
    vm: AppViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onLoginRequest: () -> Unit
) {
    val login by vm.login.collectAsState()
    val library by vm.library.collectAsState()
    var settingsOpen by remember { mutableStateOf(false) }
    val profile = login.profile
    val liked = (library.created + library.collected).firstOrNull { it.isLiked }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ChromeClearance)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CodaTheme.pagePadding, end = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的", fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { settingsOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "设置",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Personal hero (same component family as the playlist hero).
        item {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                val avatarSize = minOf(maxWidth * 0.68f, 300.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile != null && profile.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(avatarSize / 2)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        profile?.nickname ?: "我的",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (profile != null) "网易云音乐" else "登录网易云",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (profile != null) {
                            "${library.created.size + library.collected.size} 歌单"
                        } else {
                            "同步我喜欢的音乐、歌单与收藏"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    if (profile == null) {
                        Button(
                            onClick = onLoginRequest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CodaTheme.accent
                            )
                        ) {
                            Text("登录网易云", color = Color.White)
                        }
                    } else if (liked != null) {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground)
                                .clickable { onOpenPlaylist(liked.id) }
                                .padding(horizontal = 28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "我喜欢的音乐",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }

        if (profile != null) {
            if (library.created.isNotEmpty()) {
                item {
                    SectionHeader(
                        "创建的歌单",
                        modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(library.created) { playlist ->
                            PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.id) })
                        }
                    }
                    Spacer(Modifier.height(CodaTheme.sectionSpacing))
                }
            }
            item {
                SectionHeader(
                    "收藏的歌单",
                    modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                )
                Spacer(Modifier.height(16.dp))
                if (library.collected.isEmpty()) {
                    Text(
                        "暂无收藏歌单",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = CodaTheme.pagePadding)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = CodaTheme.pagePadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(library.collected) { playlist ->
                            PlaylistCard(playlist, onClick = { onOpenPlaylist(playlist.id) })
                        }
                    }
                }
                Spacer(Modifier.height(CodaTheme.sectionSpacing))
            }
        }
    }

    if (settingsOpen) {
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("设置") },
            text = {
                Column {
                    Text(
                        "账号管理",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (profile != null) "网易云音乐 · ${profile.nickname}" else "网易云音乐 · 未登录",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "关于",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Coda for Android · 版本 0.1.0", fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "非官方第三方客户端，音乐内容与数据来自网易云音乐，版权归原权利人所有。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (profile != null) {
                    TextButton(onClick = {
                        vm.logout()
                        settingsOpen = false
                    }) {
                        Text("退出登录", color = CodaTheme.accent)
                    }
                } else {
                    TextButton(onClick = {
                        settingsOpen = false
                        onLoginRequest()
                    }) {
                        Text("登录", color = CodaTheme.accent)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsOpen = false }) { Text("完成") }
            },
            shape = RoundedCornerShape(CodaTheme.Radius.panel)
        )
    }
}
