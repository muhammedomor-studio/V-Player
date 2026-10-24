package studio.muhammedomor.vplayer.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import studio.muhammedomor.vplayer.data.MediaParcel
import studio.muhammedomor.vplayer.data.PlaybackPreferences

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var items = listOf<MediaParcel>()
    private var currentIndex = 0

    private lateinit var rootContainer: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var header: LinearLayout
    private lateinit var controls: LinearLayout
    private lateinit var titleView: TextView

    private lateinit var prefs: PlaybackPreferences
    private var isLocked = false
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    private val subtitleLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadExternalSubtitle(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PlaybackPreferences(this)

        @Suppress("DEPRECATION")
        items = intent.getParcelableArrayListExtra<MediaParcel>("media")?.toList().orEmpty()
        currentIndex = intent.getIntExtra("index", 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))

        if (items.isEmpty()) {
            Toast.makeText(this, "No media provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        buildPlayerUI()
        playCurrentMedia()
    }

    private fun buildPlayerUI() {
        rootContainer = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }

        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            resizeMode = currentResizeMode
        }
        rootContainer.addView(playerView, FrameLayout.LayoutParams(-1, -1))

        val gestureOverlay = GestureOverlayView(this, object : GestureOverlayView.GestureListener {
            override fun onDoubleTapLeft() {
                player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                Toast.makeText(this@PlayerActivity, "−10s", Toast.LENGTH_SHORT).show()
            }
            override fun onDoubleTapRight() {
                player?.let { p -> p.seekTo((p.currentPosition + 10_000).coerceAtMost(p.duration.coerceAtLeast(0))) }
                Toast.makeText(this@PlayerActivity, "+10s", Toast.LENGTH_SHORT).show()
            }
            override fun onVerticalSwipeLeft(deltaY: Float) {
                adjustBrightness(deltaY)
            }
            override fun onVerticalSwipeRight(deltaY: Float) {
                adjustVolume(deltaY)
            }
        })
        rootContainer.addView(gestureOverlay, FrameLayout.LayoutParams(-1, -1))

        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            weightSum = 1f
        }

        header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(0x88000000.toInt())
        }
        titleView = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 15f
            maxLines = 1
        }
        header.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(controlButton("CC") { subtitleLauncher.launch("*/*") })
        header.addView(controlButton("Fit") { toggleScaleMode() })
        header.addView(controlButton("PiP") { enterPipMode() })
        header.addView(controlButton("✕") { finish() })

        overlayLayout.addView(header, LinearLayout.LayoutParams(-1, -2))
        
        val spacer = View(this)
        overlayLayout.addView(spacer, LinearLayout.LayoutParams(-1, 0, 1f))

        controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(0x88000000.toInt())
        }
        controls.addView(controlButton("|◀") { playAt(currentIndex - 1) })
        controls.addView(controlButton("Speed") { showSpeedDialog() })
        controls.addView(controlButton(if (isLocked) "Unlock" else "Lock") { toggleLock() })
        controls.addView(controlButton("▶|") { playAt(currentIndex + 1) })

        overlayLayout.addView(controls, LinearLayout.LayoutParams(-1, -2))

        rootContainer.addView(overlayLayout, FrameLayout.LayoutParams(-1, -1))
        setContentView(rootContainer)
        applyImmersiveMode()
    }

    private fun playCurrentMedia() {
        if (items.isEmpty()) return
        val item = items[currentIndex]
        titleView.text = item.name

        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                playerView.player = exo
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            if (currentIndex < items.lastIndex) playAt(currentIndex + 1)
                        }
                    }
                })
            }
        }

        val exoMediaItem = ExoMediaItem.fromUri(item.uri)
        player?.apply {
            setMediaItem(exoMediaItem)
            prepare()
            val savedPos = prefs.getSavedPosition(item.uri.toString())
            if (savedPos > 3_000L) seekTo(savedPos)
            playWhenReady = true
        }
    }

    private fun playAt(index: Int) {
        saveCurrentPosition()
        currentIndex = index.coerceIn(0, items.lastIndex)
        playCurrentMedia()
    }

    private fun loadExternalSubtitle(uri: Uri) {
        val subtitle = SubtitleConfiguration.Builder(uri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val currentItem = items[currentIndex]
        val exoItem = ExoMediaItem.Builder()
            .setUri(currentItem.uri)
            .setSubtitleConfigurations(listOf(subtitle))
            .build()

        player?.let { p ->
            val pos = p.currentPosition
            p.setMediaItem(exoItem)
            p.prepare()
            p.seekTo(pos)
            p.playWhenReady = true
        }
        Toast.makeText(this, "Subtitle loaded", Toast.LENGTH_SHORT).show()
    }

    private fun toggleScaleMode() {
        currentResizeMode = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        playerView.resizeMode = currentResizeMode
        val modeName = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
            else -> "Fit"
        }
        Toast.makeText(this, "Scale Mode: $modeName", Toast.LENGTH_SHORT).show()
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.25x", "0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x", "3.0x", "4.0x")
        val speedValues = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
        AlertDialog.Builder(this)
            .setTitle("Playback Speed")
            .setItems(speeds) { _, which ->
                player?.setPlaybackSpeed(speedValues[which])
            }
            .show()
    }

    private fun toggleLock() {
        isLocked = !isLocked
        header.visibility = if (isLocked) View.GONE else View.VISIBLE
        controls.visibility = if (isLocked) View.GONE else View.VISIBLE
        Toast.makeText(this, if (isLocked) "Screen Locked" else "Screen Unlocked", Toast.LENGTH_SHORT).show()
    }

    private fun adjustBrightness(deltaY: Float) {
        val layoutParams = window.attributes
        var brightness = layoutParams.screenBrightness
        if (brightness < 0) brightness = 0.5f
        brightness = (brightness + deltaY / 1000f).coerceIn(0.01f, 1.0f)
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    private fun adjustVolume(deltaY: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val change = if (deltaY > 0) 1 else -1
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, change, 0)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            Toast.makeText(this, "PiP not supported on this Android version", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentPosition() {
        player?.let { p ->
            if (p.currentPosition > 0 && currentIndex in items.indices) {
                prefs.savePosition(items[currentIndex].uri.toString(), p.currentPosition)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && player?.isPlaying == true) {
            enterPipMode()
        }
    }

    override fun onPause() {
        saveCurrentPosition()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            player?.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        saveCurrentPosition()
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun controlButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0x33FFFFFF.toInt())
        setOnClickListener { action() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
