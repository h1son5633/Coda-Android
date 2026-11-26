package com.h1son5633.coda.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.h1son5633.coda.net.NeteaseApi
import com.h1son5633.coda.net.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerUiState(
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queueSize: Int = 0,
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val shuffle: Boolean = false,
    /** Set when NetEase refuses a play URL (login/VIP/copyright gate). */
    val unavailableSong: Song? = null
)

/**
 * Owns the queue and talks to [PlaybackService] through a MediaController.
 * Play URLs are resolved per song right before playback (they are signed and
 * short-lived), and gated songs are skipped with a surfaced notice.
 */
class PlayerController(
    context: Context,
    private val api: NeteaseApi,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private val queue = mutableListOf<Song>()
    private var index = -1
    private var consecutiveMisses = 0
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { attachListeners(it) }
        }, MoreExecutors.directExecutor())
    }

    private fun attachListeners(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = syncFromPlayer()
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onTrackEnded()
                syncFromPlayer()
            }
        })
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                syncFromPlayer()
                delay(250)
            }
        }
    }

    private fun syncFromPlayer() {
        val player = controller ?: return
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: _state.value.durationMs
        )
    }

    fun queueSnapshot(): List<Song> = queue.toList()

    fun jumpTo(target: Int) {
        if (target !in queue.indices) return
        index = target
        consecutiveMisses = 0
        playCurrent()
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        queue.clear()
        queue.addAll(songs)
        index = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        consecutiveMisses = 0
        playCurrent()
    }

    fun playSong(song: Song) = playQueue(listOf(song), 0)

    private fun playCurrent() {
        val song = queue.getOrNull(index) ?: return
        _state.value = _state.value.copy(
            current = song,
            queueSize = queue.size,
            queueIndex = index,
            positionMs = 0,
            durationMs = song.durationMs,
            unavailableSong = null
        )
        scope.launch {
            val url = api.songUrl(song.id)
            if (url == null) {
                consecutiveMisses++
                _state.value = _state.value.copy(unavailableSong = song, isPlaying = false)
                // Skip forward through gated songs, but never loop forever.
                if (consecutiveMisses < queue.size && queue.size > 1) {
                    index = (index + 1) % queue.size
                    playCurrent()
                }
                return@launch
            }
            consecutiveMisses = 0
            val item = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(song.artists)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.picUrl.takeIf { it.isNotBlank() }?.toUri())
                        .build()
                )
                .build()
            controller?.apply {
                setMediaItem(item)
                prepare()
                play()
            }
        }
    }

    private fun onTrackEnded() {
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> {
                controller?.seekTo(0)
                controller?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> next(auto = true)
        }
    }

    fun toggle() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next(auto: Boolean = false) {
        if (queue.isEmpty()) return
        if (!auto && _state.value.shuffle && queue.size > 1) {
            var candidate = index
            while (candidate == index) candidate = queue.indices.random()
            index = candidate
        } else {
            if (index >= queue.size - 1) {
                if (_state.value.repeatMode == RepeatMode.OFF && auto) return
                index = 0
            } else {
                index++
            }
        }
        consecutiveMisses = 0
        playCurrent()
    }

    fun previous() {
        if (queue.isEmpty()) return
        // Like every major player: >3s in means restart, else go back.
        if ((controller?.currentPosition ?: 0) > 3000) {
            controller?.seekTo(0)
            return
        }
        index = if (index <= 0) queue.size - 1 else index - 1
        consecutiveMisses = 0
        playCurrent()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun cycleRepeat() {
        _state.value = _state.value.copy(
            repeatMode = when (_state.value.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
        )
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(shuffle = !_state.value.shuffle)
    }

    fun dismissUnavailable() {
        _state.value = _state.value.copy(unavailableSong = null)
    }
}
