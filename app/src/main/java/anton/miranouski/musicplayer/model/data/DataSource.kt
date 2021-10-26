package anton.miranouski.musicplayer.model.data

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import androidx.core.net.toUri
import anton.miranouski.musicplayer.model.data.State.STATE_CREATED
import anton.miranouski.musicplayer.model.data.State.STATE_ERROR
import anton.miranouski.musicplayer.model.data.State.STATE_INITIALIZED
import anton.miranouski.musicplayer.model.data.State.STATE_INITIALIZING
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DataSource @Inject constructor(private val repository: Repository) {

    var tracks = emptyList<MediaMetadataCompat>()

    suspend fun getMediaMetadata(context: Context) = withContext(Dispatchers.IO) {
        state = STATE_INITIALIZING

        tracks = repository.getAllTracks(context).map { track ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_TITLE, track.title)
                .putString(METADATA_KEY_ARTIST, track.artist)
                .putString(METADATA_KEY_ALBUM_ART_URI, track.bitmapUri)
                .putString(METADATA_KEY_MEDIA_URI, track.trackUri)
                .putString(METADATA_KEY_MEDIA_ID, track.trackUri)
                .build()
        }

        state = STATE_INITIALIZED
    }

    fun getMediaItems() = tracks.map { track ->
        val description = MediaDescriptionCompat.Builder()
            .setTitle(track.description.title)
            .setSubtitle(track.description.subtitle)
            .setIconUri(track.description.iconUri)
            .setMediaUri(track.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setMediaId(track.description.mediaId)
            .build()

        MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
    }.toMutableList()

    fun getMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()

        tracks.forEach { track ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(track.getString(METADATA_KEY_MEDIA_URI).toUri())

            concatenatingMediaSource.addMediaSource(mediaSource)
        }

        return concatenatingMediaSource
    }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun isReady(action: (Boolean) -> Unit): Boolean {
        return if (state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action
            false
        } else {
            action(state == STATE_INITIALIZED)
            true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
