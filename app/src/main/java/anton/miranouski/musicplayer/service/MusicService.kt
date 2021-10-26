package anton.miranouski.musicplayer.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import anton.miranouski.musicplayer.model.data.DataSource
import anton.miranouski.musicplayer.util.Constants.MEDIA_ROOT_ID
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var dataSource: DataSource

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var currentPlayingTrack: MediaMetadataCompat? = null

    private var isPLayerInitialized = false

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            dataSource.getMediaMetadata(this@MusicService)
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        val playbackPreparer = PlaybackPreparer(dataSource) {
            currentPlayingTrack = it

            preparePlayer(
                dataSource.tracks,
                it,
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(playbackPreparer)
            setQueueNavigator(QueueNavigator())
            setPlayer(exoPlayer)
        }
    }

    private fun preparePlayer(
        tracks: List<MediaMetadataCompat>,
        trackToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val currentTrackIndex = if (currentPlayingTrack == null) 0 else tracks.indexOf(trackToPlay)
        exoPlayer.apply {
            prepare(dataSource.getMediaSource(dataSourceFactory))
            seekTo(currentTrackIndex, 0L)
            playWhenReady = playNow
        }
    }

    private inner class QueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return dataSource.tracks[windowIndex].description
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultSent = dataSource.isReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(dataSource.getMediaItems())
                        if (!isPLayerInitialized && dataSource.tracks.isNotEmpty()) {
                            preparePlayer(
                                dataSource.tracks,
                                dataSource.tracks[0],
                                false
                            )
                            isPLayerInitialized = true
                        }
                    } else {
                        result.sendResult(null)
                    }
                }
                if (!resultSent) {
                    result.detach()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.release()
    }
}
