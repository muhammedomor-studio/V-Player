package studio.muhammedomor.vplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MediaAdapter
    private var allMedia = listOf<MediaItem>()
    private var mode = "All"
    private var query = ""
    private var selectedFolder: String? = null
    private val prefs by lazy { getSharedPreferences("vplayer", MODE_PRIVATE) }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refreshLibrary() }
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            Toast.makeText(this, "Folder access granted. Media scan refreshed.", Toast.LENGTH_LONG).show()
            refreshLibrary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(240,241,244)
        window.navigationBarColor = android.graphics.Color.rgb(250,250,250)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()
        requestMediaAccess()
    }

    private lateinit var list: RecyclerView
    private lateinit var count: TextView
    private lateinit var title: TextView
    private lateinit var search: EditText
    private lateinit var empty: TextView

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(10), dp(18), dp(8)); setBackgroundColor(0xFFF6F7FB.toInt()) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply { text = "V PLAYER"; textSize = 30f; setTextColor(0xFF171923.toInt()); typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, dp(72), 1f))
        header.addView(button("◷") { showHistory() })
        header.addView(button("⌕") { search.requestFocus() })
        root.addView(header)
        search = EditText(this).apply { hint = "Search videos and music"; setSingleLine(true); textSize = 15f; setPadding(dp(14), 0, dp(14), 0); backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFF4B1B.toInt()); setTextColor(0xFF202124.toInt()) }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(12)) }
        listOf("All", "Playlists", "Folders", "Shorts").forEach { label -> tabs.addView(button(label) { mode = label; selectedFolder = null; title.text = if(label == "All") "Videos" else label; render() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3),0,dp(3),0) }) }
        root.addView(tabs)
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title = TextView(this).apply { text = "Videos"; textSize = 25f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(0xFF202124.toInt()) }
        titleRow.addView(title, LinearLayout.LayoutParams(0, dp(48), 1f))
        titleRow.addView(button("↻") { refreshLibrary() })
        titleRow.addView(button("＋") { folderPicker.launch(null) })
        root.addView(titleRow)
        count = TextView(this).apply { textSize = 13f; setTextColor(0xFF888A90.toInt()); setPadding(0,0,0,dp(8)) }
        root.addView(count)
        list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity); overScrollMode = View.OVER_SCROLL_NEVER }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        empty = TextView(this).apply { text = "No media found\nAllow media access or add files to your phone."; textSize = 17f; gravity = Gravity.CENTER; setTextColor(0xFF85878C.toInt()); visibility = View.GONE }
        root.addView(empty, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        listOf("Videos", "Music", "▶", "Downloads", "More").forEach { label -> bottom.addView(button(label) {
            when(label) {
                "Music" -> { mode = "Music"; title.text = "Music"; render() }
                "Videos" -> { mode = "All"; title.text = "Videos"; render() }
                "More" -> showMore()
                "Downloads" -> showDownloads()
                else -> if(allMedia.isNotEmpty()) openPlayer(0)
            }
        }, LinearLayout.LayoutParams(0, dp(52), 1f)) }
        root.addView(bottom)
        setContentView(root)
        adapter = MediaAdapter { item ->
            if (mode == "Folders" && selectedFolder == null) {
                selectedFolder = item.name
                query = ""
                search.setText("")
                title.text = item.name
                render()
            } else {
                val originalIndex = allMedia.indexOfFirst { it.uri == item.uri }
                openPlayer(originalIndex)
            }
        }
        list.adapter = adapter
        search.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s?.toString().orEmpty(); render() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (mode == "Folders" && selectedFolder != null) {
            selectedFolder = null
            title.text = "Folders"
            render()
        } else {
            super.onBackPressed()
        }
    }

    private fun requestMediaAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(this).setTitle("Allow media access").setMessage("V Player needs file access to list media stored on your device. You can also grant normal media permissions only.").setPositiveButton("Allow all files") { _, _ ->
                try { startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))) } catch (_: Exception) { requestNormalPermissions() }
            }.setNegativeButton("Media only") { _, _ -> requestNormalPermissions() }.setOnCancelListener { requestNormalPermissions() }.show()
        } else requestNormalPermissions()
    }
    private fun requestNormalPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
            Build.VERSION.SDK_INT >= 23 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            else -> emptyArray()
        }
        if (permissions.isNotEmpty() && permissions.any { ContextCompat.checkSelfPermission(this,it) != PackageManager.PERMISSION_GRANTED }) permissionLauncher.launch(permissions) else refreshLibrary()
    }
    private fun refreshLibrary() { allMedia = MediaRepository.scan(this); render() }
    private fun render() {
        if (!::adapter.isInitialized) return
        var shown = when(mode) {
            "Music" -> allMedia.filter { it.isAudio }
            "Shorts" -> allMedia.filter { !it.isAudio && it.durationMs in 1..90_000 }
            "Folders" -> {
                val videos = allMedia.filter { !it.isAudio }
                if (selectedFolder == null) {
                    val folders = videos.groupBy { it.folder }.toSortedMap()
                    val folderRows = folders.mapNotNull { (folder, media) -> media.firstOrNull()?.copy(name = folder) }
                    // Folder rows are opened by their folder name; media is filtered after selection.
                    folderRows
                } else videos.filter { it.folder == selectedFolder }
            }
            "Playlists" -> allMedia.filter { prefs.getStringSet("favorites", emptySet()).orEmpty().contains(it.uri.toString()) }
            else -> allMedia.filter { !it.isAudio }
        }
        shown = shown.filter { it.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault())) }
        adapter.submit(shown)
        count.text = "${shown.size} ${if(shown.size == 1) "item" else "items"}"
        empty.visibility = if(shown.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if(shown.isEmpty()) View.GONE else View.VISIBLE
    }
    private fun openPlayer(index: Int) { if(index < 0 || index >= allMedia.size) return; startActivity(Intent(this, PlayerActivity::class.java).putParcelableArrayListExtra("media", ArrayList(allMedia.map { MediaParcel.from(it) })).putExtra("index", index)) }
    private fun showHistory() {
        val ids = prefs.getString("history", "").orEmpty().split("|").filter { it.isNotBlank() }
        val recent = ids.mapNotNull { id -> allMedia.find { it.uri.toString() == id } }.distinctBy { it.uri }.take(30)
        if(recent.isEmpty()) { AlertDialog.Builder(this).setMessage("No recently played videos yet.").setPositiveButton("OK",null).show(); return }
        AlertDialog.Builder(this).setTitle("Recently played").setItems(recent.map { it.name }.toTypedArray()) { _, which -> openPlayer(allMedia.indexOf(recent[which])) }.show()
    }
    private fun showDownloads() { AlertDialog.Builder(this).setTitle("Downloads").setMessage("V Player plays local files. Downloading online videos is not included.").setPositiveButton("OK",null).show() }
    private fun showMore() {
        val opts = arrayOf("Favorites", "Choose folder", "Settings", "Developer profile", "GitHub repository", "About V Player")
        AlertDialog.Builder(this).setTitle("V Player • More").setItems(opts) { _, which -> when(which) {
            0 -> { mode = "Playlists"; title.text = "Favorites"; render() }
            1 -> folderPicker.launch(null)
            2 -> AlertDialog.Builder(this).setTitle("Settings").setItems(arrayOf("Clear playback history","Storage permissions","App information")) { _, item ->
                when(item) {
                    0 -> { prefs.edit().remove("history").apply(); Toast.makeText(this,"History cleared",Toast.LENGTH_SHORT).show() }
                    1 -> showPermissionsInfo()
                    2 -> showAbout()
                }
            }.show()
            3 -> openExternal("https://hikmah.net/@muhammedomor_official")
            4 -> openExternal("https://github.com/muhammedomor-studio/V-Player")
            5 -> showAbout()
        } }.show()
    }
    private fun openExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show() }
    }
    private fun showPermissionsInfo() { AlertDialog.Builder(this).setTitle("Storage access").setMessage("Android may restrict broad file access. V Player requests media permissions and offers the system All files access setting. Grant only permissions you are comfortable with.").setPositiveButton("OK",null).show() }
    private fun showAbout() { AlertDialog.Builder(this).setTitle("About V Player").setMessage("V Player\\nVersion 1.0.0\\nDeveloper: Muhammed Omor\\n\\nHikmah profile:\\nhttps://hikmah.net/@muhammedomor_official\\n\\nGitHub:\\nhttps://github.com/muhammedomor-studio/V-Player").setPositiveButton("OK",null).show() }
    private fun button(label: String, action: () -> Unit): Button = Button(this).apply { text = label; textSize = 12f; isAllCaps = false; setTextColor(0xFF303136.toInt()); setBackgroundTintList(android.content.res.ColorStateList.valueOf(if(label in listOf("All","Videos")) 0xFFFF4B1B.toInt() else 0xFFE9EBF2.toInt())); minHeight = dp(42); setPadding(dp(5),0,dp(5),0); setOnClickListener { action() } }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
