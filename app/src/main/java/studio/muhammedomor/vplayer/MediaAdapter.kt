package studio.muhammedomor.vplayer

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MediaAdapter(private val onClick: (MediaItem) -> Unit): RecyclerView.Adapter<MediaAdapter.Holder>() {
    private var items = listOf<MediaItem>()
    fun submit(newItems: List<MediaItem>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(0, 8, 0, 8) }
        val thumb = TextView(ctx).apply { text = "▶"; textSize = 30f; gravity = android.view.Gravity.CENTER; setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xFFB9C3D0.toInt()) }
        row.addView(thumb, LinearLayout.LayoutParams((ctx.resources.displayMetrics.widthPixels*0.37f).toInt(), (ctx.resources.displayMetrics.widthPixels*0.21f).toInt()).apply { rightMargin = 14 })
        val column = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(ctx).apply { textSize = 16f; maxLines = 2; setTextColor(0xFF202124.toInt()) }
        val meta = TextView(ctx).apply { textSize = 13f; setTextColor(0xFF888A90.toInt()); setPadding(0,5,0,0) }
        column.addView(title); column.addView(meta)
        row.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        return Holder(row, title, meta, onClick)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
    class Holder(view: android.view.View, private val title: TextView, private val meta: TextView, private val onClick: (MediaItem) -> Unit): RecyclerView.ViewHolder(view) {
        fun bind(item: MediaItem) { title.text = item.name; meta.text = "${if(item.isAudio) "Audio" else "Video"} · ${formatSize(item.sizeBytes)} · ${item.folder}"; itemView.setOnClickListener { onClick(item) } }
        private fun formatSize(n: Long): String = if(n >= 1024L*1024*1024) String.format(Locale.US,"%.1f GB",n/1024.0/1024/1024) else String.format(Locale.US,"%.0f MB",n/1024.0/1024)
    }
}
