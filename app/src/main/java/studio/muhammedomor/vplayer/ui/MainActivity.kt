package studio.muhammedomor.vplayer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import studio.muhammedomor.vplayer.data.MediaItem
import studio.muhammedomor.vplayer.data.MediaParcel
import studio.muhammedomor.vplayer.data.MediaRepository
import studio.muhammedomor.vplayer.data.PlaybackPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MediaAdapter
    private var allMedia = listOf<MediaItem>()
    private var currentTab = "All"
    private var searchQuery = ""
    private var isGridView = false

    private lateinit var listRecyclerView: RecyclerView
    private lateinit var countText: TextView
    private lateinit var titleText: TextView
    private lateinit var searchEdit: EditText
    private lateinit var emptyText: TextView

    private val prefs by lazy { PlaybackPreferences(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshMediaLibrary() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
        checkAndRequestPermissions()
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(0xFF0F111A.toInt())
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(TextView(this).apply {
            text = "VPLAYER"
            textSize = 26f
            setTextColor(0xFFFF4B1B.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, dp(56), 1f))

        header.addView(iconButton("◷") { showHistoryDialog() })
        header.addView(iconButton("⊞") { toggleViewMode() })
        root.addView(header)

        searchEdit = EditText(this).apply {
            hint = "Search videos..."
            setHintTextColor(0x88FFFFFF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(0xFF1E2230.toInt())
        }
        root.addView(searchEdit, LinearLayout.LayoutParams(-1, -2))

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        listOf("All", "Folders", "Audio").forEach { tab ->
            tabs.addView(tabButton(tab) {
                currentTab = tab
                render()
            }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        root.addView(tabs)

        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleText = TextView(this).apply {
            text = "Media Library"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        titleRow.addView(titleText, LinearLayout.LayoutParams(0, -2, 1f))

        countText = TextView(this).apply {
            textSize = 12f
            setTextColor(0x88FFFFFF.toInt())
        }
        titleRow.addView(countText)
        root.addView(titleRow)

        listRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        root.addView(listRecyclerView, LinearLayout.LayoutParams(-1, 0, 1f))

        emptyText = TextView(this).apply {
            text = "No videos found.
Grant media permissions to scan local storage."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0x88FFFFFF.toInt())
            visibility = View.GONE
        }
        root.addView(emptyText, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)

        adapter = MediaAdapter { selectedItem ->
            val index = allMedia.indexOfFirst { it.uri == selectedItem.uri }
            if (index >= 0) openPlayer(index)
        }
        listRecyclerView.adapter = adapter

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                render()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun checkAndRequestPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        } else {
            refreshMediaLibrary()
        }
    }

    private fun refreshMediaLibrary() {
        lifecycleScope.launch {
            allMedia = MediaRepository.scanMedia(this@MainActivity)
            render()
        }
    }

    private fun render() {
        var filtered = when (currentTab) {
            "Audio" -> allMedia.filter { it.isAudio }
            "Folders" -> allMedia.filter { !it.isAudio }
            else -> allMedia.filter { !it.isAudio }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        adapter.submit(filtered)
        countText.text = "${filtered.size} items"
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        listRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun toggleViewMode() {
        isGridView = !isGridView
        listRecyclerView.layoutManager = if (isGridView) GridLayoutManager(this, 2) else LinearLayoutManager(this)
    }

    private fun openPlayer(index: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putParcelableArrayListExtra("media", ArrayList(allMedia.map { MediaParcel.from(it) }))
            putExtra("index", index)
        }
        startActivity(intent)
    }

    private fun showHistoryDialog() {
        val historyUris = prefs.getHistory()
        val recentItems = historyUris.mapNotNull { uri -> allMedia.find { it.uri.toString() == uri } }

        if (recentItems.isEmpty()) {
            Toast.makeText(this, "No recently played videos", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Recently Played")
            .setItems(recentItems.map { it.name }.toTypedArray()) { _, which ->
                val selected = recentItems[which]
                val idx = allMedia.indexOfFirst { it.uri == selected.uri }
                if (idx >= 0) openPlayer(idx)
            }
            .setNegativeButton("Clear") { _, _ -> prefs.clearHistory() }
            .show()
    }

    private fun tabButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF1E2230.toInt())
        setOnClickListener { action() }
    }

    private fun iconButton(symbol: String, action: () -> Unit) = Button(this).apply {
        text = symbol
        textSize = 16f
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0x00000000)
        setOnClickListener { action() }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
