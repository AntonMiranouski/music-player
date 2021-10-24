package anton.miranouski.musicplayer.model.data

import android.content.Context
import anton.miranouski.musicplayer.model.Track
import anton.miranouski.musicplayer.util.getJsonDataFromAsset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Repository {

    fun getAllTracks(context: Context): List<Track> {
        val jsonData = getJsonDataFromAsset(context, DATA)
        val type = object : TypeToken<List<Track>>() {}.type

        return Gson().fromJson(jsonData, type)
    }

    companion object {

        private const val DATA = "playlist.json"
    }
}
