package studio.muhammedomor.vplayer.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val folder: String,
    val dateAddedMs: Long
) {
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
}
