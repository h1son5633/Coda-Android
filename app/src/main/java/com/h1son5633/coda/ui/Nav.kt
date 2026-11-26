package com.h1son5633.coda.ui

/** Destinations pushed inside a tab's own stack (mirrors the original's NavigationStack per tab). */
sealed interface Dest {
    data class PlaylistDetail(val id: Long) : Dest
    data class AlbumDetail(val id: Long) : Dest
    data class ArtistDetail(val id: Long) : Dest
    data object DailySongs : Dest
    data object Toplists : Dest
}
