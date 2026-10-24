package studio.muhammedomor.vplayer.data

import android.content.Context
import android.content.SharedPreferences

class PlaybackPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vplayer_prefs", Context.MODE_PRIVATE)

    fun savePosition(uriStr: String, positionMs: Long) {
        prefs.edit().putLong("pos_$uriStr", positionMs).apply()
        addToHistory(uriStr)
    }

    fun getSavedPosition(uriStr: String): Long {
        return prefs.getLong("pos_$uriStr", 0L)
    }

    private fun addToHistory(uriStr: String) {
        val history = getHistory().toMutableList()
        history.remove(uriStr)
        history.add(0, uriStr)
        prefs.edit().putString("history_list", history.take(50).joinToString("|")).apply()
    }

    fun getHistory(): List<String> {
        val raw = prefs.getString("history_list", "").orEmpty()
        return if (raw.isBlank()) emptyList() else raw.split("|")
    }

    fun clearHistory() {
        prefs.edit().remove("history_list").apply()
    }
}
