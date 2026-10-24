package studio.muhammedomor.vplayer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object MediaRepository {
    fun scan(context: Context): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATA
        )
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString()
        )
        val sort = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        try {
            resolver.query(collection, projection, selection, args, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Untitled media"
                    val mime = cursor.getString(mimeCol) ?: continue
                    val uri = ContentUris.withAppendedId(collection, id)
                    val path = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                    val folder = path.substringBeforeLast("/", "Device media").substringAfterLast("/")
                    result += MediaItem(id, uri, name, cursor.getLong(durationCol), cursor.getLong(sizeCol), mime, folder)
                }
            }
        } catch (_: SecurityException) { }
        return result
    }
}
