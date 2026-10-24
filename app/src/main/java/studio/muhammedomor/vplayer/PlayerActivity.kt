package studio.muhammedomor.vplayer

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlin.math.abs

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var items = listOf<MediaParcel>()
    private var currentIndex = 0
    private lateinit var header: LinearLayout
    private lateinit var controls: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private val prefs by lazy { getSharedPreferences("vplayer", MODE_PRIVATE) }
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private var downX = 0f
    private var downY = 0f
    private var gestureStartPosition = 0L
    private var gestureUsed = false
    private var aspectMode = 0
    private val subtitlePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) attachSubtitle(uri)
    }
    private fun key(item: MediaParcel) = "position_${item.uri}"

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = intent.getParcelableArrayListExtra<MediaParcel>("media")?.toList().orEmpty()
        currentIndex = intent.getIntExtra("index", 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (items.isEmpty()) { finish(); return }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF080A10.toInt()) }
        header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), 0, dp(8), 0); setBackgroundColor(0xFF111522.toInt()) }
        titleView = TextView(this).apply { setTextColor(0xFFFFFFFF.toInt()); textSize = 14f; maxLines = 2; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        header.addView(titleView, LinearLayout.LayoutParams(0, dp(50), 1f))
        header.addView(controlButton("↔", false) { toggleOrientation() })
        header.addView(controlButton("PiP", true) { enterPip() })
        header.addView(controlButton("✕", false) { finish() })
        root.addView(header)

        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            controllerAutoShow = true
            setBackgroundColor(0xFF000000.toInt())
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        playerView.setOnTouchListener { _, event -> handleGesture(event) }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))

        controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(dp(2), dp(5), dp(2), dp(6)); setBackgroundColor(0xFF111522.toInt()) }
        listOf(
            "Prev" to { playAt((currentIndex - 1).coerceAtLeast(0)) },
            "−10s" to { seekBy(-10_000) },
            "+10s" to { seekBy(10_000) },
            "Speed" to { chooseSpeed() },
            "Tracks" to { chooseAudioTrack() },
            "Subs" to { subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream")) },
            "Fit" to { chooseAspect() }
        ).forEach { (label, action) -> controls.addView(controlButton(label, label == "Speed", action), LinearLayout.LayoutParams(0, dp(44), 1f)) }
        root.addView(controls)
        setContentView(root)
        applyOrientationLayout()
        playAt(currentIndex)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun controlButton(label: String, accent: Boolean, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 11f; isAllCaps = false; minHeight = dp(42); minWidth = dp(40)
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(if (accent) 0xFFFF4B1B.toInt() else 0xFF272D3D.toInt())
        setPadding(dp(2), 0, dp(2), 0)
        setOnClickListener { action() }
    }

    private fun seekBy(delta: Long) {
        player?.let { p ->
            val target = (p.currentPosition + delta).coerceAtLeast(0L)
            p.seekTo(if (p.duration > 0) target.coerceAtMost(p.duration) else target)
        }
    }

    private fun handleGesture(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                gestureStartPosition = player?.currentPosition ?: 0L
                gestureUsed = false
                return false
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > dp(70) && abs(dx) > abs(dy) * 1.25f) {
                    val delta = (dx / dp(8)).toLong() * 1_000L
                    player?.let { p ->
                        val target = (gestureStartPosition + delta).coerceAtLeast(0L)
                        p.seekTo(if (p.duration > 0) target.coerceAtMost(p.duration) else target)
                    }
                    Toast.makeText(this, if (dx > 0) "Forward" else "Rewind", Toast.LENGTH_SHORT).show()
                    gestureUsed = true
                    return true
                }
                if (abs(dy) > dp(45) && abs(dy) > abs(dx) * 1.2f) {
                    if (downX < playerView.width / 2f) {
                        val attrs = window.attributes
                        val current = if (attrs.screenBrightness < 0) 0.5f else attrs.screenBrightness
                        attrs.screenBrightness = (current - dy / playerView.height).coerceIn(0.05f, 1f)
                        window.attributes = attrs
                        Toast.makeText(this, "Brightness ${((attrs.screenBrightness) * 100).toInt()}%", Toast.LENGTH_SHORT).show()
                    } else {
                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val change = (-(dy / playerView.height) * max * 2).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (current + change).coerceIn(0, max), 0)
                    }
                    gestureUsed = true
                    return true
                }
            }
        }
        return false
    }

    private fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
            } catch (_: Exception) { Toast.makeText(this, "Picture-in-picture is unavailable on this device", Toast.LENGTH_SHORT).show() }
        } else Toast.makeText(this, "PiP requires Android 8.0 or later", Toast.LENGTH_SHORT).show()
    }

    private fun chooseAspect() {
        val modes = arrayOf("Fit", "Crop", "Stretch", "16:9", "4:3")
        AlertDialog.Builder(this).setTitle("Video scaling").setSingleChoiceItems(modes, aspectMode) { dialog, which ->
            aspectMode = which
            playerView.resizeMode = when (which) {
                1 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                2 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            dialog.dismiss()
        }.show()
    }

    private fun chooseAudioTrack() {
        val p = player ?: return
        val groups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (groups.isEmpty()) {
            Toast.makeText(this, "No selectable audio tracks found", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = mutableListOf<String>()
        val choices = mutableListOf<Pair<TrackGroup, Int>>()
        groups.forEachIndexed { gi, group ->
            for (i in 0 until group.length) {
                val f = group.getTrackFormat(i)
                labels += "${f.language ?: "Audio"} · ${f.label ?: "Track ${i + 1}"}"
                choices += group.mediaTrackGroup to i
            }
        }
        AlertDialog.Builder(this).setTitle("Audio track").setItems(labels.toTypedArray()) { _, which ->
            val (group, track) = choices[which]
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setOverrideForType(TrackSelectionOverride(group, listOf(track))).build()
        }.setNegativeButton("Auto", null).show()
    }

    private fun attachSubtitle(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
        val mime = when (uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)) {
            "vtt" -> MimeTypes.TEXT_VTT
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.TEXT_UNKNOWN
        }
        val subtitle = ExoMediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mime)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val item = items.getOrNull(currentIndex) ?: return
        player?.setMediaItem(ExoMediaItem.Builder().setUri(item.uri).setSubtitleConfigurations(listOf(subtitle)).build())
        player?.prepare()
        player?.seekTo(prefs.getLong(key(item), 0L))
        player?.playWhenReady = true
        Toast.makeText(this, "Subtitle loaded", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun applyOrientationLayout() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        header.visibility = if (landscape) View.GONE else View.VISIBLE
        controls.visibility = if (landscape) View.GONE else View.VISIBLE
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::header.isInitialized) applyOrientationLayout()
    }

    override fun onUserLeaveHint() { super.onUserLeaveHint(); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) enterPip() }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::header.isInitialized) { header.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE; controls.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE }
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
        val speeds = floatArrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f)
        val labels = speeds.map { "${it}×" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Playback speed")
            .setItems(labels) { _, which ->
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
    override fun onPause() { savePosition(); if (!isInPictureInPictureMode) player?.pause(); super.onPause() }
    override fun onStop() { savePosition(); super.onStop() }
    override fun onDestroy() { savePosition(); playerView.player = null; player?.release(); player = null; super.onDestroy() }
}
