package com.beatwave.music.desktop

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which of the app's dialogs, sheets and full-page overlays are showing.
 */
@Stable
internal class DesktopOverlays {

    /** The signal-chain readout behind the player's quality badge. */
    var pipeline by mutableStateOf(false)

    /** Which device playback is sent to. */
    var audioOutput by mutableStateOf(false)

    /** The party this device is listening with. */
    var listenTogether by mutableStateOf(false)
    var songMenu by mutableStateOf(false)
    var downloadManager by mutableStateOf(false)
    var replay by mutableStateOf(false)
    var playlistDialog by mutableStateOf(false)
    var rename by mutableStateOf(false)
    var delete by mutableStateOf(false)
    var nowPlaying by mutableStateOf(false)

    /** Quick Audio & Crossfade modal over the player */
    var audioQuickDialog by mutableStateOf(false)

    /** Floating Equalizer dialog */
    var equalizerDialog by mutableStateOf(false)

    /**
     * Which page of Settings fills the page area, or none.
     */
    var settingsPage by mutableStateOf<DesktopSettingsPage?>(null)
    var lastfmLogin by mutableStateOf(false)
    var listenBrainzToken by mutableStateOf(false)
    var discordToken by mutableStateOf(false)
    var accounts by mutableStateOf(false)
    var signIn by mutableStateOf(false)

    /** The column beside the page with the lyrics or the queue in it, or none. */
    var sidePanel by mutableStateOf<DesktopSidePanel?>(null)

    /** Opens [panel] in the column, or puts the column away if [panel] is what it already shows. */
    fun toggleSidePanel(panel: DesktopSidePanel) {
        sidePanel = if (sidePanel == panel) null else panel
    }
}

/**
 * Settings and the pages its rows lead to.
 */
internal enum class DesktopSettingsPage {
    MAIN,
    EQUALIZER,
    AUDIO_OUTPUT,
    LISTEN_TOGETHER,
    LYRICS_SOURCES,
    TRANSLATION_LANGUAGE,
    SPOTIFY_CANVAS,
    INTEGRATIONS,
    LICENSES,
    ABOUT,
}
