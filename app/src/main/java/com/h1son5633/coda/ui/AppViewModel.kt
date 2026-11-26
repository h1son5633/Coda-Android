package com.h1son5633.coda.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.h1son5633.coda.CodaApp
import com.h1son5633.coda.data.LyricLine
import com.h1son5633.coda.data.Lyrics
import com.h1son5633.coda.net.AccountProfile
import com.h1son5633.coda.net.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val error: String? = null
)

data class HomeUiState(
    val personalized: List<com.h1son5633.coda.net.Playlist> = emptyList(),
    val toplists: List<com.h1son5633.coda.net.Playlist> = emptyList(),
    val toplistPreviews: Map<Long, List<Song>> = emptyMap(),
    val newAlbums: List<com.h1son5633.coda.net.Album> = emptyList(),
    val dailySongs: List<Song> = emptyList(),
    val loading: Boolean = true
)

data class LibraryUiState(
    val created: List<com.h1son5633.coda.net.Playlist> = emptyList(),
    val collected: List<com.h1son5633.coda.net.Playlist> = emptyList(),
    val loading: Boolean = false
)

enum class QrStatus { IDLE, WAITING, SCANNED, EXPIRED, SUCCESS, FAILED }

data class LoginUiState(
    val profile: AccountProfile? = null,
    val qrContent: String? = null,
    val qrStatus: QrStatus = QrStatus.IDLE
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CodaApp
    val api get() = app.api
    val player get() = app.player

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home.asStateFlow()

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()

    private var searchJob: Job? = null
    private var qrJob: Job? = null
    private var lyricJob: Job? = null
    private var lyricSongId: Long = -1

    init {
        refreshProfile()
        loadHome()
        // Load lyrics whenever the playing song changes.
        viewModelScope.launch {
            player.state.collect { state ->
                val song = state.current
                if (song != null && song.id != lyricSongId) {
                    lyricSongId = song.id
                    loadLyrics(song.id)
                }
            }
        }
    }

    // ---- Search ----------------------------------------------------------

    fun onQueryChange(query: String) {
        _search.value = _search.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _search.value = _search.value.copy(results = emptyList(), searched = false, error = null)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runSearch(query)
        }
    }

    fun submitSearch() {
        val query = _search.value.query
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(query) }
    }

    private suspend fun runSearch(query: String) {
        _search.value = _search.value.copy(loading = true, error = null)
        try {
            val results = api.searchSongs(query)
            if (_search.value.query == query) {
                _search.value = _search.value.copy(
                    results = results, loading = false, searched = true
                )
            }
        } catch (e: Exception) {
            if (_search.value.query == query) {
                _search.value = _search.value.copy(
                    loading = false, searched = true, error = "搜索失败，请检查网络"
                )
            }
        }
    }

    fun loadMoreSongs() {
        val current = _search.value
        if (current.loading || !current.searched || current.query.isBlank()) return
        viewModelScope.launch {
            _search.value = current.copy(loading = true)
            try {
                val more = api.searchSongs(
                    current.query, limit = 30, offset = current.results.size
                )
                if (_search.value.query == current.query) {
                    _search.value = _search.value.copy(
                        results = current.results + more, loading = false
                    )
                }
            } catch (e: Exception) {
                _search.value = _search.value.copy(loading = false)
            }
        }
    }

    // ---- Lyrics ----------------------------------------------------------

    private fun loadLyrics(songId: Long) {
        lyricJob?.cancel()
        _lyrics.value = emptyList()
        lyricJob = viewModelScope.launch {
            try {
                _lyrics.value = Lyrics.parse(api.lyric(songId))
            } catch (e: Exception) {
                _lyrics.value = emptyList()
            }
        }
    }

    // ---- Login -----------------------------------------------------------

    fun refreshProfile() {
        viewModelScope.launch {
            val profile = try {
                api.accountProfile()
            } catch (e: Exception) {
                null
            }
            _login.value = _login.value.copy(profile = profile)
            if (profile != null) {
                loadLibrary(profile.userId)
                loadDaily()
            }
        }
    }

    // ---- Home / library ----------------------------------------------------

    fun loadHome() {
        viewModelScope.launch {
            _home.value = _home.value.copy(loading = true)
            val personalized = try { api.personalizedPlaylists() } catch (e: Exception) { emptyList() }
            val toplists = try { api.toplists() } catch (e: Exception) { emptyList() }
            val albums = try { api.newAlbums() } catch (e: Exception) { emptyList() }
            _home.value = _home.value.copy(
                personalized = personalized, toplists = toplists,
                newAlbums = albums, loading = false
            )
            // Top-10 previews for the first few charts (the original shows
            // ranked previews inline on Home).
            val previews = mutableMapOf<Long, List<Song>>()
            for (chart in toplists.take(4)) {
                try {
                    api.playlistDetail(chart.id)?.let { previews[chart.id] = it.second.take(10) }
                } catch (e: Exception) { /* leave empty */ }
            }
            _home.value = _home.value.copy(toplistPreviews = previews)
        }
    }

    private fun loadDaily() {
        viewModelScope.launch {
            val daily = try { api.dailySongs() } catch (e: Exception) { emptyList() }
            _home.value = _home.value.copy(dailySongs = daily)
        }
    }

    fun loadLibrary(uid: Long) {
        viewModelScope.launch {
            _library.value = _library.value.copy(loading = true)
            val all = try { api.userPlaylists(uid) } catch (e: Exception) { emptyList() }
            _library.value = LibraryUiState(
                created = all.filter { it.userId == uid },
                collected = all.filter { it.userId != uid },
                loading = false
            )
        }
    }

    fun startQrLogin() {
        qrJob?.cancel()
        _login.value = _login.value.copy(qrContent = null, qrStatus = QrStatus.WAITING)
        qrJob = viewModelScope.launch {
            try {
                val unikey = api.qrUnikey()
                if (unikey == null) {
                    _login.value = _login.value.copy(qrStatus = QrStatus.FAILED)
                    return@launch
                }
                _login.value = _login.value.copy(
                    qrContent = "https://music.163.com/login?codekey=$unikey",
                    qrStatus = QrStatus.WAITING
                )
                // Poll for up to ~4 minutes (the original polls every 1.2s).
                repeat(160) {
                    delay(1500)
                    when (api.qrPoll(unikey)) {
                        803 -> {
                            _login.value = _login.value.copy(
                                qrStatus = QrStatus.SUCCESS, qrContent = null
                            )
                            refreshProfile()
                            return@launch
                        }
                        802 -> _login.value = _login.value.copy(qrStatus = QrStatus.SCANNED)
                        800 -> {
                            _login.value = _login.value.copy(
                                qrStatus = QrStatus.EXPIRED, qrContent = null
                            )
                            return@launch
                        }
                    }
                }
                _login.value = _login.value.copy(qrStatus = QrStatus.EXPIRED, qrContent = null)
            } catch (e: Exception) {
                _login.value = _login.value.copy(qrStatus = QrStatus.FAILED)
            }
        }
    }

    fun cancelQrLogin() {
        qrJob?.cancel()
        _login.value = _login.value.copy(qrContent = null, qrStatus = QrStatus.IDLE)
    }

    fun logout() {
        app.accountStore.clearCookies()
        _login.value = LoginUiState()
    }
}
