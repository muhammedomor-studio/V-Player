package studio.muhammedomor.vplayer

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import java.util.concurrent.Executors
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MediaAdapter(private val onClick: (MediaItem) -> Unit): RecyclerView.Adapter<MediaAdapter.Holder>() {
    private val executor = Executors.newFixedThreadPool(2)
    private var items = listOf<MediaItem>()
    fun submit(newItems: List<MediaItem>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0, 8, 0, 8) }
        val thumb = ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(0xFF252A36.toInt()); setImageResource(android.R.drawable.ic_media_play) }
        row.addView(thumb, LinearLayout.LayoutParams((ctx.resources.displayMetrics.widthPixels*0.37f).toInt(), (ctx.resources.displayMetrics.widthPixels*0.21f).toInt()).apply { rightMargin = 14 })
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(ctx).apply { textSize = 16f; maxLines = 2; setTextColor(0xFF202124.toInt()) }
        val meta = TextView(ctx).apply { textSize = 13f; setTextColor(0xFF888A90.toInt()); setPadding(0,5,0,0) }
        column.addView(title); column.addView(meta)
        row.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        return Holder(row, thumb, title, meta, onClick, executor)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
    class Holder(view: android.view.View, private val thumb: ImageView, private val title: TextView, private val meta: TextView, private val onClick: (MediaItem) -> Unit, private val executor: java.util.concurrent.ExecutorService): RecyclerView.ViewHolder(view) {
        fun bind(item: MediaItem) {
            title.text = item.name
            meta.text = "${if(item.isAudio) "Audio" else "Video"} · ${formatSize(item.sizeBytes)} · ${item.folder}"
            thumb.tag = item.uri.toString()
            thumb.setImageResource(android.R.drawable.ic_media_play)
            if (Build.VERSION.SDK_INT >= 29 && !item.isAudio) {
                executor.execute {
                    try {
                        val bitmap = thumb.context.contentResolver.loadThumbnail(item.uri, Size(480, 270), null)
                        thumb.post { if (thumb.tag == item.uri.toString()) thumb.setImageBitmap(bitmap) }
                    } catch (_: Exception) { }
                }
            }
            itemView.setOnClickListener { onClick(item) }
        }
        private fun formatSize(n: Long): String = if(n >= 1024L*1024*1024) String.format(Locale.US,"%.1f GB",n/1024.0/1024/1024) else String.format(Locale.US,"%.0f MB",n/1024.0/1024)
    }
}
