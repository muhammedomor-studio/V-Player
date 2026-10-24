package studio.muhammedomor.vplayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
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
    private lateinit var header: LinearLayout
    private lateinit var controls: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private val prefs by lazy { getSharedPreferences("vplayer", MODE_PRIVATE) }
    private fun key(item: MediaParcel) = "position_${item.uri}"

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = intent.getParcelableArrayListExtra<MediaParcel>("media")?.toList().orEmpty()
        currentIndex = intent.getIntExtra("index", 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (items.isEmpty()) { finish(); return }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF080A10.toInt()) }
        header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(12), 0); setBackgroundColor(0xFF111522.toInt()) }
        titleView = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 15f; maxLines = 2; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        header.addView(titleView, LinearLayout.LayoutParams(0, dp(54), 1f))
        header.addView(controlButton("⤢", true) {
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        })
        header.addView(controlButton("✕", false) { finish() })
        root.addView(header)

        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            controllerAutoShow = true
            setBackgroundColor(0xFF000000.toInt())
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))

        controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(dp(4), dp(6), dp(4), dp(8)); setBackgroundColor(0xFF111522.toInt()) }
        controls.addView(controlButton("|◀", false) { playAt((currentIndex - 1).coerceAtLeast(0)) })
        controls.addView(controlButton("−10s", false) { player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) } })
        controls.addView(controlButton("+10s", false) { player?.let { p -> val d = p.duration; p.seekTo(if (d > 0) (p.currentPosition + 10_000).coerceAtMost(d) else p.currentPosition + 10_000) } })
        controls.addView(controlButton("1×", true) { chooseSpeed() })
        controls.addView(controlButton("▶|", false) { playAt((currentIndex + 1).coerceAtMost(items.lastIndex)) })
        root.addView(controls)
        setContentView(root)
        applyOrientationLayout()
        playAt(currentIndex)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun controlButton(label: String, accent: Boolean, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false; minHeight = dp(42); minWidth = dp(44)
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(if (accent) 0xFFFF4B1B.toInt() else 0xFF272D3D.toInt())
        setOnClickListener { action() }
    }

    @Suppress("DEPRECATION")
    private fun applyOrientationLayout() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        header.visibility = if (landscape) View.GONE else View.VISIBLE
        controls.visibility = if (landscape) View.GONE else View.VISIBLE
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        playerView.layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::header.isInitialized) applyOrientationLayout()
    }

    private fun playAt(index: Int) {
        if (items.isEmpty()) return
        savePosition()
        currentIndex = index.coerceIn(0, items.lastIndex)
        val item = items[currentIndex]
        titleView.text = item.name
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_ENDED) savePosition() }
                })
            }
        }
        player?.apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            prepare()
            val resume = prefs.getLong(key(item), 0L)
            if (resume > 3_000L) seekTo(resume)
            playWhenReady = true
        }
        prefs.edit().putString("last_uri", item.uri.toString()).apply()
        val history = prefs.getString("history", "").orEmpty().split("|").filter { it.isNotBlank() }.toMutableList()
        history.remove(item.uri.toString()); history.add(0, item.uri.toString())
        prefs.edit().putString("history", history.take(50).joinToString("|")).apply()
    }

    private fun chooseSpeed() {
        val speeds = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Playback speed")
            .setItems(arrayOf("0.5×", "0.75×", "1×", "1.25×", "1.5×", "1.75×", "2×")) { _, which ->
                player?.setPlaybackSpeed(speeds[which])
                Toast.makeText(this, "Speed ${String.format(Locale.US, "%.2f", speeds[which])}×", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun savePosition() {
        val p = player ?: return
        val item = items.getOrNull(currentIndex) ?: return
        val position = p.currentPosition
        if (position > 0) prefs.edit().putLong(key(item), position).putLong("last_position", position).apply()
    }
    override fun onPause() { savePosition(); player?.pause(); super.onPause() }
    override fun onStop() { savePosition(); super.onStop() }
    override fun onDestroy() { savePosition(); playerView.player = null; player?.release(); player = null; super.onDestroy() }
}
