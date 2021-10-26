package anton.miranouski.musicplayer.ui

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.ViewModel
import anton.miranouski.musicplayer.service.MusicServiceConnection
import anton.miranouski.musicplayer.util.Constants.MEDIA_ROOT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val currentPlayingTrack = musicServiceConnection.currentPlayingTrack
    val playbackState = musicServiceConnection.playbackState

    init {
        musicServiceConnection.subscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                }
            }
        )
    }

    fun toNextTrack() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun toPreviousTrack() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun playOrPause() {
        if (playbackState.value?.state == PlaybackStateCompat.STATE_PAUSED) {
            musicServiceConnection.transportControls.play()
        } else {
            musicServiceConnection.transportControls.pause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
    }
}
