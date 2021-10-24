package anton.miranouski.musicplayer.util

import android.content.Context
import android.util.Log
import java.io.IOException

private const val TAG = "JsonDataFromAsset"

fun getJsonDataFromAsset(context: Context, fileName: String): String? {
    val jsonString: String

    try {
        jsonString = context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
    } catch (e: IOException) {
        Log.e(TAG, e.stackTraceToString())
        return null
    }

    return jsonString
}
