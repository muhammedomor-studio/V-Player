package studio.muhammedomor.vplayer.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaParcel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val folder: String,
    val dateAddedMs: Long
) : Parcelable {
    fun toMediaItem() = MediaItem(id, uri, name, durationMs, sizeBytes, mimeType, folder, dateAddedMs)

    companion object {
        fun from(m: MediaItem) = MediaParcel(
            m.id, m.uri, m.name, m.durationMs, m.sizeBytes, m.mimeType, m.folder, m.dateAddedMs
        )
    }
}
