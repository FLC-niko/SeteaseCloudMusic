package com.example.seteasecloudmusic.core.player

import androidx.media3.common.Player
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundPlaybackLifecycleTest {

    private fun sampleTrack(id: Long = 1001L): Track = Track(
        id = id,
        title = "测试曲目",
        durationMs = 210_000L,
        artists = listOf(Artist(id = 1L, name = "测试歌手")),
        album = Album(id = 2L, title = "测试专辑")
    )

    @Test
    fun resolveTrackSwitchPreparationAction_returnsNoneWhenAutoAdvancingAfterTrackEnded() {
        val action = MusicPlayerController.resolveTrackSwitchPreparationAction(
            isDifferentTrack = true,
            hasImmediatePlayableUrl = false,
            currentPlayerState = Player.STATE_ENDED,
            isPlayerPlaying = false,
            isPlayerPlayWhenReady = true
        )
        assertEquals(MusicPlayerController.TrackSwitchPreparationAction.NONE, action)
    }

    @Test
    fun resolveTrackSwitchPreparationAction_returnsNoneForImmediatePrefetchedUrl() {
        val action = MusicPlayerController.resolveTrackSwitchPreparationAction(
            isDifferentTrack = true,
            hasImmediatePlayableUrl = true,
            currentPlayerState = Player.STATE_READY,
            isPlayerPlaying = true,
            isPlayerPlayWhenReady = true
        )
        assertEquals(MusicPlayerController.TrackSwitchPreparationAction.NONE, action)
    }

    @Test
    fun resolveTrackSwitchPreparationAction_pausesPreviousWhenSwitchingToUnprefetchedTrack() {
        val action = MusicPlayerController.resolveTrackSwitchPreparationAction(
            isDifferentTrack = true,
            hasImmediatePlayableUrl = false,
            currentPlayerState = Player.STATE_READY,
            isPlayerPlaying = true,
            isPlayerPlayWhenReady = true
        )
        assertEquals(MusicPlayerController.TrackSwitchPreparationAction.PAUSE_PREVIOUS, action)
    }

    @Test
    fun resolveTrackSwitchPreparationAction_returnsNoneForSameTrack() {
        val action = MusicPlayerController.resolveTrackSwitchPreparationAction(
            isDifferentTrack = false,
            hasImmediatePlayableUrl = false,
            currentPlayerState = Player.STATE_READY,
            isPlayerPlaying = true,
            isPlayerPlayWhenReady = true
        )
        assertEquals(MusicPlayerController.TrackSwitchPreparationAction.NONE, action)
    }

    @Test
    fun shouldIgnoreStalePlayerStateEvent_ignoresOldTrackReadyDuringBuffering() {
        assertTrue(
            MusicPlayerController.shouldIgnoreStalePlayerStateEvent(
                playbackState = Player.STATE_READY,
                currentStatus = PlayerStatus.BUFFERING,
                activeMediaId = "1001",
                currentTrackId = 2002L
            )
        )
        assertTrue(
            MusicPlayerController.shouldIgnoreStalePlayerStateEvent(
                playbackState = Player.STATE_READY,
                currentStatus = PlayerStatus.ERROR,
                activeMediaId = "1001",
                currentTrackId = 2002L
            )
        )
    }

    @Test
    fun shouldIgnoreStalePlayerStateEvent_allowsNewTrackReadyAndNeverIgnoresEnded() {
        assertFalse(
            MusicPlayerController.shouldIgnoreStalePlayerStateEvent(
                playbackState = Player.STATE_READY,
                currentStatus = PlayerStatus.BUFFERING,
                activeMediaId = "2002",
                currentTrackId = 2002L
            )
        )
        assertFalse(
            MusicPlayerController.shouldIgnoreStalePlayerStateEvent(
                playbackState = Player.STATE_ENDED,
                currentStatus = PlayerStatus.BUFFERING,
                activeMediaId = "1001",
                currentTrackId = 2002L
            )
        )
        assertTrue(
            MusicPlayerController.shouldIgnoreStalePlayerStateEvent(
                playbackState = Player.STATE_IDLE,
                currentStatus = PlayerStatus.BUFFERING,
                activeMediaId = "2002",
                currentTrackId = 2002L
            )
        )
    }

    @Test
    fun mapExposedPlaybackStateAndPlayWhenReady_masksEndedAndIdleDuringTransition() {
        assertEquals(
            Player.STATE_BUFFERING,
            MusicService.mapExposedPlaybackState(Player.STATE_ENDED, PlayerStatus.BUFFERING)
        )
        assertEquals(
            Player.STATE_BUFFERING,
            MusicService.mapExposedPlaybackState(Player.STATE_IDLE, PlayerStatus.PLAYING)
        )
        assertEquals(
            Player.STATE_ENDED,
            MusicService.mapExposedPlaybackState(Player.STATE_ENDED, PlayerStatus.ENDED)
        )
        assertEquals(
            Player.STATE_ENDED,
            MusicService.mapExposedPlaybackState(Player.STATE_ENDED, PlayerStatus.PAUSED)
        )

        assertTrue(MusicService.mapExposedPlayWhenReady(false, PlayerStatus.BUFFERING))
        assertFalse(MusicService.mapExposedPlayWhenReady(false, PlayerStatus.PLAYING))
        assertTrue(MusicService.mapExposedPlayWhenReady(true, PlayerStatus.PLAYING))
        assertFalse(MusicService.mapExposedPlayWhenReady(false, PlayerStatus.PAUSED))
    }

    @Test
    fun shouldShowPauseActionAndReleasePlaybackLocks_respectRealPauseWhilePreservingBuffering() {
        assertTrue(MusicService.shouldShowPauseAction(playerPlayWhenReady = true, controllerStatus = PlayerStatus.PLAYING))
        assertTrue(MusicService.shouldShowPauseAction(playerPlayWhenReady = false, controllerStatus = PlayerStatus.BUFFERING))
        assertFalse(MusicService.shouldShowPauseAction(playerPlayWhenReady = false, controllerStatus = PlayerStatus.PLAYING))
        assertFalse(MusicService.shouldShowPauseAction(playerPlayWhenReady = false, controllerStatus = PlayerStatus.PAUSED))

        assertFalse(MusicService.shouldReleasePlaybackLocks(playerPlayWhenReady = false, controllerStatus = PlayerStatus.BUFFERING))
        assertFalse(MusicService.shouldReleasePlaybackLocks(playerPlayWhenReady = true, controllerStatus = PlayerStatus.PLAYING))
        assertTrue(MusicService.shouldReleasePlaybackLocks(playerPlayWhenReady = false, controllerStatus = PlayerStatus.PLAYING))
        assertTrue(MusicService.shouldReleasePlaybackLocks(playerPlayWhenReady = false, controllerStatus = PlayerStatus.PAUSED))
    }

    @Test
    fun shouldKeepServiceAliveOnTaskRemoved_keepsAliveDuringPlayingOrBuffering() {
        assertTrue(
            MusicService.shouldKeepServiceAliveOnTaskRemoved(
                hasPlayer = true,
                playerPlayWhenReady = false,
                playerMediaItemCount = 0,
                controllerStatus = PlayerStatus.BUFFERING,
                hasCurrentTrack = true
            )
        )
        assertTrue(
            MusicService.shouldKeepServiceAliveOnTaskRemoved(
                hasPlayer = true,
                playerPlayWhenReady = true,
                playerMediaItemCount = 1,
                controllerStatus = PlayerStatus.PLAYING,
                hasCurrentTrack = true
            )
        )
        assertFalse(
            MusicService.shouldKeepServiceAliveOnTaskRemoved(
                hasPlayer = true,
                playerPlayWhenReady = false,
                playerMediaItemCount = 1,
                controllerStatus = PlayerStatus.PAUSED,
                hasCurrentTrack = true
            )
        )
        assertFalse(
            MusicService.shouldKeepServiceAliveOnTaskRemoved(
                hasPlayer = true,
                playerPlayWhenReady = false,
                playerMediaItemCount = 0,
                controllerStatus = PlayerStatus.IDLE,
                hasCurrentTrack = false
            )
        )
    }

    @Test
    fun shouldExitForegroundForIdleEmptyQueue_onlyWhenCompletelyIdleAndEmpty() {
        assertTrue(
            MusicService.shouldExitForegroundForIdleEmptyQueue(
                state = PlaybackState(
                    status = PlayerStatus.IDLE,
                    currentTrack = null,
                    queueTracks = emptyList()
                ),
                isPlayerPlaying = false
            )
        )
        assertFalse(
            MusicService.shouldExitForegroundForIdleEmptyQueue(
                state = PlaybackState(
                    status = PlayerStatus.IDLE,
                    currentTrack = sampleTrack(),
                    queueTracks = listOf(sampleTrack())
                ),
                isPlayerPlaying = false
            )
        )
        assertFalse(
            MusicService.shouldExitForegroundForIdleEmptyQueue(
                state = PlaybackState(
                    status = PlayerStatus.PAUSED,
                    currentTrack = null,
                    queueTracks = emptyList()
                ),
                isPlayerPlaying = false
            )
        )
        assertFalse(
            MusicService.shouldExitForegroundForIdleEmptyQueue(
                state = PlaybackState(
                    status = PlayerStatus.IDLE,
                    currentTrack = null,
                    queueTracks = emptyList()
                ),
                isPlayerPlaying = true
            )
        )
    }

    @Test
    fun resolveDisplayedMediaText_preservesTrackTitleAndArtistWhenBluetoothLyricsDisabled() {
        val displayed = MusicService.resolveDisplayedMediaText(
            trackTitle = "七里香",
            artist = "周杰伦",
            lyric = "窗外的麻雀 在电线杆上多嘴",
            isPlaying = true,
            bluetoothLyricsEnabled = false
        )
        assertEquals("七里香", displayed.title)
        assertEquals("周杰伦", displayed.artist)
        assertEquals("周杰伦", displayed.subtitle)
    }

    @Test
    fun resolveDisplayedMediaText_swapsLyricIntoTitleOnlyWhenBluetoothLyricsEnabledAndPlaying() {
        val playingDisplayed = MusicService.resolveDisplayedMediaText(
            trackTitle = "七里香",
            artist = "周杰伦",
            lyric = "窗外的麻雀 在电线杆上多嘴",
            isPlaying = true,
            bluetoothLyricsEnabled = true
        )
        assertEquals("窗外的麻雀 在电线杆上多嘴", playingDisplayed.title)
        assertEquals("七里香", playingDisplayed.artist)
        assertEquals("窗外的麻雀 在电线杆上多嘴", playingDisplayed.subtitle)

        val pausedDisplayed = MusicService.resolveDisplayedMediaText(
            trackTitle = "七里香",
            artist = "周杰伦",
            lyric = "窗外的麻雀 在电线杆上多嘴",
            isPlaying = false,
            bluetoothLyricsEnabled = true
        )
        assertEquals("七里香", pausedDisplayed.title)
        assertEquals("周杰伦", pausedDisplayed.artist)

        val blankLyricDisplayed = MusicService.resolveDisplayedMediaText(
            trackTitle = "七里香",
            artist = "周杰伦",
            lyric = "   ",
            isPlaying = true,
            bluetoothLyricsEnabled = true
        )
        assertEquals("七里香", blankLyricDisplayed.title)
        assertEquals("周杰伦", blankLyricDisplayed.artist)
    }
}
