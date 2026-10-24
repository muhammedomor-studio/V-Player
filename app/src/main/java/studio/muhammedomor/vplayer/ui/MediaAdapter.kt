package studio.muhammedomor.vplayer.ui

import android.os.Build
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import studio.muhammedomor.vplayer.data.MediaItem
import java.util.Locale
import java.util.concurrent.Executors

class MediaAdapter(
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    private var items = listOf<MediaItem>()
    private val executor = Executors.newFixedThreadPool(3)

    fun submit(newItems: List<MediaItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val thumb = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF1E2230.toInt())
            setImageResource(android.R.drawable.ic_media_play)
        }
        val thumbWidth = (ctx.resources.displayMetrics.widthPixels * 0.30f).toInt()
        val thumbHeight = (thumbWidth * 0.56f).toInt()
        row.addView(thumb, LinearLayout.LayoutParams(thumbWidth, thumbHeight).apply { rightMargin = 16 })

        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(ctx).apply {
            textSize = 15f
            maxLines = 2
            setTextColor(0xFFFFFFFF.toInt())
        }
        val meta = TextView(ctx).apply {
            textSize = 12f
            setTextColor(0x88FFFFFF.toInt())
            setPadding(0, 4, 0, 0)
        }
        column.addView(title)
        column.addView(meta)

        row.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        return ViewHolder(row, thumb, title, meta, onClick, executor)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        view: View,
        private val thumb: ImageView,
        private val title: TextView,
        private val meta: TextView,
        private val onClick: (MediaItem) -> Unit,
        private val executor: java.util.concurrent.ExecutorService
    ) : ViewHolder(view) {

        fun bind(item: MediaItem) {
            title.text = item.name
            meta.text = "${formatSize(item.sizeBytes)} • ${item.folder}"
            thumb.tag = item.uri.toString()
            thumb.setImageResource(android.R.drawable.ic_media_play)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !item.isAudio) {
                executor.execute {
                    try {
                        val bitmap = thumb.context.contentResolver.loadThumbnail(
                            item.uri, Size(320, 180), null
                        )
                        thumb.post {
                            if (thumb.tag == item.uri.toString()) {
                                thumb.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            itemView.setOnClickListener { onClick(item) }
        }

        private fun formatSize(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format(Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.0f MB", mb)
            }
        }
    }
}
