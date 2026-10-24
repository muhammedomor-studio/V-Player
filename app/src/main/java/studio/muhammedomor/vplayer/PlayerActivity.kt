package studio.muhammedomor.vplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var items = listOf<MediaParcel>()
    private var currentIndex = 0
    private val prefs by lazy { getSharedPreferences("vplayer", MODE_PRIVATE) }
    private fun key(item: MediaParcel) = "position_${item.uri}"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = intent.getParcelableArrayListExtra<MediaParcel>("media")?.toList().orEmpty()
        currentIndex = intent.getIntExtra("index", 0).coerceIn(0, (items.size-1).coerceAtLeast(0))
        if(items.isEmpty()) { finish(); return }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF050505.toInt()) }
        val header = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(10,0,10,0) }
        val title = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 15f; maxLines = 1 }
        header.addView(title, LinearLayout.LayoutParams(0, 48, 1f))
        header.addView(Button(this).apply { text = "↻"; setOnClickListener { requestedOrientation = if(resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE } })
        header.addView(Button(this).apply { text = "×"; setOnClickListener { finish() } })
        root.addView(header)
        val view = PlayerView(this).apply { useController = true; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); controllerAutoShow = true; setBackgroundColor(0xFF000000.toInt()) }
        root.addView(view, LinearLayout.LayoutParams(-1,0,1f))
        val controls = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER }
        controls.addView(Button(this).apply { text = "−10s"; setOnClickListener { player?.let { it.seekTo((it.currentPosition-10_000).coerceAtLeast(0)) } } })
        controls.addView(Button(this).apply { text = "+10s"; setOnClickListener { player?.let { it.seekTo((it.currentPosition+10_000).coerceAtMost(it.duration.coerceAtLeast(0))) } } })
        controls.addView(Button(this).apply { text = "Speed"; setOnClickListener { chooseSpeed() } })
        controls.addView(Button(this).apply { text = "Next"; setOnClickListener { playAt((currentIndex+1)%items.size, view, title) } })
        root.addView(controls)
        setContentView(root)
        playAt(currentIndex, view, title)
    }
    private fun playAt(index: Int, view: PlayerView, title: TextView) {
        if(items.isEmpty()) return
        savePosition()
        currentIndex = index.coerceIn(0,items.lastIndex)
        val item = items[currentIndex]
        title.text = item.name
        if(player == null) {
            player = ExoPlayer.Builder(this).build().also { view.player = it
                it.addListener(object: Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) { if(playbackState == Player.STATE_ENDED) savePosition() }
                })
            }
        }
        player?.apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
            val resume = prefs.getLong(key(item),0L)
            if(resume > 3000L) seekTo(resume)
            playWhenReady = true
        }
        prefs.edit().putString("last_uri",item.uri.toString()).apply()
        val history = prefs.getString("history","").orEmpty().split("|").filter { it.isNotBlank() }.toMutableList()
        history.remove(item.uri.toString()); history.add(0,item.uri.toString())
        prefs.edit().putString("history",history.take(50).joinToString("|")).apply()
    }
    private fun chooseSpeed() {
        val speeds = floatArrayOf(0.5f,0.75f,1f,1.25f,1.5f,1.75f,2f)
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Playback speed").setItems(arrayOf("0.5×","0.75×","1×","1.25×","1.5×","1.75×","2×")) { _, which ->
            player?.setPlaybackSpeed(speeds[which]); Toast.makeText(this,"Speed ${String.format(Locale.US,"%.2f",speeds[which])}×",Toast.LENGTH_SHORT).show()
        }.show()
    }
    private fun savePosition() {
        val p = player ?: return
        val item = items.getOrNull(currentIndex) ?: return
        val position = p.currentPosition
        if(position > 0) prefs.edit().putLong(key(item),position).putLong("last_position",position).apply()
    }
    override fun onPause() { savePosition(); player?.pause(); super.onPause() }
    override fun onStop() { savePosition(); super.onStop() }
    override fun onDestroy() { savePosition(); player?.release(); player = null; super.onDestroy() }
}
