package anton.miranouski.musicplayer.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import anton.miranouski.musicplayer.R
import anton.miranouski.musicplayer.databinding.ActivityMainBinding
import anton.miranouski.musicplayer.model.Track
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        subscribeToObservers()

        binding.ibNext.setOnClickListener {
            mainViewModel.toNextTrack()
        }

        binding.ibPrevious.setOnClickListener {
            mainViewModel.toPreviousTrack()
        }

        binding.ibPlayPause.setOnClickListener {
            mainViewModel.playOrPause()
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.currentPlayingTrack.observe(this) { track ->
            if (track == null) return@observe

            val currentPlayingTrack = Track(
                track.description.title.toString(),
                track.description.subtitle.toString(),
                track.description.iconUri.toString(),
                track.description.mediaUri.toString()
            )

            updateTitleAndImage(currentPlayingTrack)
        }

        mainViewModel.playbackState.observe(this) {
            binding.ibPlayPause.setBackgroundResource(
                if (it?.state == PlaybackStateCompat.STATE_PAUSED) {
                    R.drawable.ic_outline_play_arrow
                } else {
                    R.drawable.ic_outline_pause
                }
            )
        }
    }

    private fun updateTitleAndImage(track: Track) {
        val title = "${track.title} - ${track.artist}"
        binding.tvTrackName.text = title

        glide.load(track.bitmapUri).into(binding.ivTrackImage)
    }
}
