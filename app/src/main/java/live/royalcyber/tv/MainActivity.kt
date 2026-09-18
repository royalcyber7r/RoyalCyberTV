package live.royalcyber.tv

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RoyalCyberTV"
    }

    // ============================================================
    // MOBILE VIEWS
    // ============================================================

    private lateinit var playerView: PlayerView
    private lateinit var playerContainer: View
    private lateinit var channelRecycler: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var searchButton: ImageButton
    private lateinit var searchBox: EditText
    private lateinit var fullscreenButton: ImageButton
    private lateinit var mainScrollView: ScrollView
    private lateinit var headerLayout: View
    private lateinit var currentChannelName: TextView
    private lateinit var channelTitle: TextView
    private lateinit var footerLayout: View
    private lateinit var bottomNavigation: View
    private lateinit var footerFacebook: ImageView
    private lateinit var footerYoutube: ImageView
    private lateinit var footerInstagram: ImageView
    private lateinit var footerTiktok: ImageView
    private lateinit var playerControls: View
    private lateinit var playPauseButton: TextView
    private lateinit var rewindButton: TextView
    private lateinit var forwardButton: TextView
    private lateinit var liveText: TextView

    private var player: ExoPlayer? = null
    private var currentChannel: Channel? = null
    private var isFullscreen = false
    private var normalPlayerHeight = 220
    private var searchWasVisible = false
    private var bottomNavigationBaseHeight = 70

    // ============================================================
    // ANDROID TV VIEWS
    // ============================================================

    private val isAndroidTV: Boolean
        get() =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_LEANBACK
            )

    private lateinit var tvChannelRecycler: RecyclerView
    private lateinit var tvUpdateButton: TextView
    private lateinit var tvBottomUpdate: TextView
    private lateinit var tvHeader: View

    private var tvSetupSuccessful = false
    private var tvPlayerView: PlayerView? = null
    private var tvPlayerContainer: FrameLayout? = null
    private var tvPlayer: ExoPlayer? = null

    // ============================================================
    // UPDATE
    // ============================================================

    private var updateCheckStarted = false

    private val handler =
        Handler(Looper.getMainLooper())

    private val hideControlsRunnable =
        Runnable {
            if (
                ::playerControls.isInitialized &&
                !isFullscreen &&
                !isFinishing &&
                !isDestroyed
            ) {
                playerControls.visibility = View.GONE
            }
        }

    // ============================================================
    // NOTIFICATION
    // ============================================================

    private val notificationPrefsName =
        "royalcyber_notification_prefs"

    private val knownChannelsKey =
        "known_channels"

    private val pendingNotificationsKey =
        "pending_notifications"

    private var channels: List<Channel> =
        emptyList()

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // ========================================================
        // ANDROID TV — STEP 1
        // ========================================================
        //
        // এই ধাপে TV-তে শুধুমাত্র TV layout খুলবে।
        //
        // RecyclerView
        // ChannelAdapter
        // JSON
        // ExoPlayer
        // Network
        //
        // কিছুই চালু হবে না।
        //
        // Mobile code নিচে আগের মতোই থাকবে।
        // ========================================================

        if (isAndroidTV) {

            setContentView(
                R.layout.activity_tv_main
            )

            return
        }

        // ========================================================
        // MOBILE CODE
        // ========================================================

        setContentView(
            R.layout.activity_main
        )

        initializeViews()

        loadChannelsFromJson()

        setupBottomNavigationInsets()

        setupChannelList()

        setupSearch()

        setupPlayer()

        setupPlayerControls()

        setupFullscreenButton()

        setupBottomMenu()

        setupSocialLinks()

        mainScrollView.post {

            if (
                !isFinishing &&
                !isDestroyed
            ) {
                updateRecyclerHeight()
            }
        }

        handler.postDelayed({

            if (
                !isFinishing &&
                !isDestroyed
            ) {
                checkForUpdateAutomatically()
            }

        }, 1500)
    }

    // ============================================================
    // CHANNEL JSON
    // ============================================================

    private fun loadChannelsFromJson() {

        Thread {

            try {

                val url =
                    java.net.URL(
                        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"
                    )

                val connection =
                    url.openConnection()
                            as java.net.HttpURLConnection

                connection.requestMethod = "GET"

                connection.connectTimeout = 10000

                connection.readTimeout = 10000

                connection.useCaches = false

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !in 200..299
                ) {
                    throw Exception(
                        "HTTP $responseCode"
                    )
                }

                val jsonText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val jsonArray =
                    JSONArray(jsonText)

                val loadedChannels =
                    mutableListOf<Channel>()

                for (
                    index in
                    0 until jsonArray.length()
                ) {

                    val item =
                        jsonArray.optJSONObject(
                            index
                        ) ?: continue

                    val name =
                        item.optString(
                            "name"
                        ).trim()

                    val logo =
                        item.optString(
                            "logo"
                        ).trim()

                    val streamUrl =
                        item.optString(
                            "streamUrl"
                        ).trim()

                    if (
                        name.isNotEmpty() &&
                        streamUrl.isNotEmpty()
                    ) {

                        loadedChannels.add(
                            Channel(
                                name,
                                logo,
                                streamUrl
                            )
                        )
                    }
                }

                Log.d(
                    TAG,
                    "Channels loaded: ${loadedChannels.size}"
                )

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }

                    channels =
                        loadedChannels

                    if (
                        ::channelAdapter.isInitialized
                    ) {
                        channelAdapter.updateList(
                            channels
                        )
                    }

                    if (
                        isAndroidTV
                    ) {

                        if (
                            tvSetupSuccessful &&
                            ::tvChannelRecycler.isInitialized
                        ) {

                            showTvLoading(false)

                            tvChannelRecycler.visibility =
                                View.VISIBLE

                            tvChannelRecycler.post {

                                if (
                                    !isFinishing &&
                                    !isDestroyed &&
                                    ::tvChannelRecycler.isInitialized
                                ) {
                                    tvChannelRecycler.requestFocus()
                                }
                            }

                        } else {

                            Log.e(
                                TAG,
                                "TV RecyclerView was not initialized"
                            )
                        }

                    } else {

                        if (
                            ::channelRecycler.isInitialized
                        ) {

                            channelRecycler.post {

                                if (
                                    !isFinishing &&
                                    !isDestroyed
                                ) {
                                    updateRecyclerHeight()
                                }
                            }
                        }
                    }

                    syncNewChannelNotifications()

                    if (
                        channels.isNotEmpty() &&
                        currentChannel == null
                    ) {

                        if (isAndroidTV) {

                            if (
                                tvSetupSuccessful &&
                                ::tvChannelRecycler.isInitialized
                            ) {

                                tvChannelRecycler.post {

                                    if (
                                        !isFinishing &&
                                        !isDestroyed
                                    ) {
                                        tvChannelRecycler.requestFocus()
                                    }
                                }
                            }

                        } else {

                            playChannel(
                                channels[0]
                            )
                        }
                    }

                    if (
                        channels.isEmpty()
                    ) {

                        if (isAndroidTV) {

                            showTvLoading(false)

                            if (
                                ::tvChannelRecycler.isInitialized
                            ) {
                                tvChannelRecycler.visibility =
                                    View.VISIBLE
                            }

                            Toast.makeText(
                                this,
                                "কোনো Channel পাওয়া যায়নি",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "channels.json loading error",
                    e
                )

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }

                    Toast.makeText(
                        this,
                        "অনলাইন channels.json লোড করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    // ============================================================
    // TV LOADING
    // ============================================================

    private fun showTvLoading(
        show: Boolean
    ) {

        if (!isAndroidTV) return

        try {

            val loadingText =
                findViewById<TextView>(
                    R.id.tv_loading_text
                )

            loadingText?.visibility =
                if (show)
                    View.VISIBLE
                else
                    View.GONE

        } catch (e: Exception) {

            Log.e(
                TAG,
                "TV loading view error",
                e
            )
        }
    }

    // ============================================================
    // TV SETUP
    // ============================================================
    //
    // STEP 1-এ এটি চালু হচ্ছে না।
    //
    // পরের ধাপে TV screen successfully খুললে
    // এখান থেকে একটি একটি করে feature চালু করব।
    // ============================================================

    private fun setupAndroidTV(): Boolean {

        return try {

            val header =
                findViewById<View>(
                    R.id.tv_header
                )

            val recycler =
                findViewById<RecyclerView>(
                    R.id.tv_channel_recycler
                )

            val updateButton =
                findViewById<TextView>(
                    R.id.tv_update_button
                )

            val bottomUpdate =
                findViewById<TextView>(
                    R.id.tv_bottom_update
                )

            if (
                header == null ||
                recycler == null ||
                updateButton == null ||
                bottomUpdate == null
            ) {

                Log.e(
                    TAG,
                    "One or more TV views are missing"
                )

                return false
            }

            tvHeader = header

            tvChannelRecycler =
                recycler

            tvUpdateButton =
                updateButton

            tvBottomUpdate =
                bottomUpdate

            tvUpdateButton.setOnClickListener {
                openUpdateScreen()
            }

            tvBottomUpdate.setOnClickListener {
                openUpdateScreen()
            }

            tvChannelRecycler.layoutManager =
                GridLayoutManager(
                    this,
                    5
                )

            tvChannelRecycler.isFocusable =
                true

            tvChannelRecycler.isFocusableInTouchMode =
                true

            tvChannelRecycler.isNestedScrollingEnabled =
                true

            tvChannelRecycler.setHasFixedSize(
                false
            )

            channelAdapter =
                ChannelAdapter(
                    channels = channels,
                    onChannelClick = {
                        channel ->
                        playTvChannel(
                            channel
                        )
                    }
                )

            tvChannelRecycler.adapter =
                channelAdapter

            tvChannelRecycler.post {

                if (
                    !isFinishing &&
                    !isDestroyed &&
                    ::tvChannelRecycler.isInitialized
                ) {

                    tvChannelRecycler.requestFocus()
                }
            }

            Log.d(
                TAG,
                "TV interface setup successful"
            )

            true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "setupAndroidTV() failed",
                e
            )

            false
        }
    }

    // ============================================================
    // TV PLAYER
    // ============================================================

    private fun setupTvPlayer(): Boolean {

        if (tvPlayer != null) {
            return true
        }

        return try {

            val contentArea =
                findViewById<FrameLayout>(
                    R.id.tv_content_area
                )

            if (contentArea == null) {

                Log.e(
                    TAG,
                    "tv_content_area not found"
                )

                return false
            }

            tvPlayer =
                ExoPlayer.Builder(
                    this
                ).build()

            tvPlayerContainer =
                FrameLayout(this)

            tvPlayerContainer?.layoutParams =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

            tvPlayerView =
                PlayerView(this)

            tvPlayerView?.layoutParams =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

            tvPlayerView?.useController =
                true

            tvPlayerView?.keepScreenOn =
                true

            tvPlayerView?.setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            tvPlayerView?.player =
                tvPlayer

            tvPlayerContainer?.addView(
                tvPlayerView
            )

            tvPlayerContainer?.visibility =
                View.GONE

            contentArea.addView(
                tvPlayerContainer
            )

            tvPlayer?.repeatMode =
                Player.REPEAT_MODE_ONE

            tvPlayer?.addListener(
                object : Player.Listener {

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        Log.e(
                            TAG,
                            "TV player error",
                            error
                        )

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }

                        Toast.makeText(
                            this@MainActivity,
                            "এই Channel চালু করা যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

            Log.d(
                TAG,
                "TV Player setup successful"
            )

            true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "setupTvPlayer() failed",
                e
            )

            try {
                tvPlayerView?.player = null
            } catch (_: Exception) {
            }

            try {
                tvPlayer?.release()
            } catch (_: Exception) {
            }

            tvPlayer = null
            tvPlayerView = null
            tvPlayerContainer = null

            Toast.makeText(
                this,
                "TV Player চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    // ============================================================
    // PLAY TV CHANNEL
    // ============================================================

    private fun playTvChannel(
        channel: Channel
    ) {

        if (
            !isAndroidTV ||
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val url =
            channel.streamUrl.trim()

        if (url.isEmpty()) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            if (tvPlayer == null) {

                val created =
                    setupTvPlayer()

                if (!created) {
                    return
                }
            }

            val exoPlayer =
                tvPlayer ?: return

            val dataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(
                        true
                    )

            val mediaSource =
                HlsMediaSource.Factory(
                    dataSourceFactory
                ).createMediaSource(
                    MediaItem.fromUri(url)
                )

            exoPlayer.stop()

            exoPlayer.clearMediaItems()

            exoPlayer.setMediaSource(
                mediaSource
            )

            exoPlayer.prepare()

            exoPlayer.playWhenReady =
                true

            currentChannel =
                channel

            if (
                ::tvChannelRecycler.isInitialized
            ) {

                tvChannelRecycler.visibility =
                    View.GONE
            }

            tvPlayerContainer?.visibility =
                View.VISIBLE

            tvPlayerView?.post {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {
                    tvPlayerView?.requestFocus()
                }
            }

            Log.d(
                TAG,
                "Playing TV channel: ${channel.name}"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "TV channel playback error: ${channel.name}",
                e
            )

            Toast.makeText(
                this,
                "এই Channel চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // CLOSE TV PLAYER
    // ============================================================

    private fun closeTvPlayer() {

        if (!isAndroidTV) return

        try {
            tvPlayer?.pause()
        } catch (_: Exception) {
        }

        tvPlayerContainer?.visibility =
            View.GONE

        if (
            ::tvChannelRecycler.isInitialized
        ) {

            tvChannelRecycler.visibility =
                View.VISIBLE

            tvChannelRecycler.post {

                if (
                    !isFinishing &&
                    !isDestroyed &&
                    ::tvChannelRecycler.isInitialized
                ) {

                    tvChannelRecycler.requestFocus()
                }
            }
        }
    }

    // ============================================================
    // BACK BUTTON
    // ============================================================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (isAndroidTV) {

            if (
                tvPlayerContainer != null &&
                tvPlayerContainer?.visibility ==
                View.VISIBLE
            ) {

                closeTvPlayer()

                return
            }
        }

        super.onBackPressed()
    }

    // ============================================================
    // NOTIFICATION
    // ============================================================

    private fun syncNewChannelNotifications() {

        try {

            val prefs =
                getSharedPreferences(
                    notificationPrefsName,
                    MODE_PRIVATE
                )

            val currentChannelNames =
                channels
                    .map {
                        it.name.trim()
                    }
                    .filter {
                        it.isNotEmpty()
                    }
                    .toSet()

            val savedKnownChannels =
                prefs.getStringSet(
                    knownChannelsKey,
                    null
                )

            if (
                savedKnownChannels == null
            ) {

                prefs.edit()
                    .putStringSet(
                        knownChannelsKey,
                        currentChannelNames
                    )
                    .putString(
                        pendingNotificationsKey,
                        JSONArray().toString()
                    )
                    .apply()

                return
            }

            val knownChannels =
                savedKnownChannels.toMutableSet()

            val pendingNotifications =
                getPendingNotifications()

            val newChannels =
                currentChannelNames.filter {
                    !knownChannels.contains(it)
                }

            newChannels.forEach {
                newChannel ->

                if (
                    !pendingNotifications.contains(
                        newChannel
                    )
                ) {

                    pendingNotifications.add(
                        newChannel
                    )
                }
            }

            knownChannels.addAll(
                currentChannelNames
            )

            prefs.edit()
                .putStringSet(
                    knownChannelsKey,
                    knownChannels
                )
                .apply()

            savePendingNotifications(
                pendingNotifications
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Notification sync error",
                e
            )
        }
    }

    private fun getPendingNotifications():
        MutableList<String> {

        val result =
            mutableListOf<String>()

        try {

            val prefs =
                getSharedPreferences(
                    notificationPrefsName,
                    MODE_PRIVATE
                )

            val raw =
                prefs.getString(
                    pendingNotificationsKey,
                    null
                )

            if (
                raw.isNullOrBlank()
            ) {
                return result
            }

            val json =
                JSONArray(raw)

            for (
                index in
                0 until json.length()
            ) {

                val name =
                    json.optString(
                        index
                    ).trim()

                if (
                    name.isNotEmpty() &&
                    !result.contains(name)
                ) {

                    result.add(name)
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Pending notification read error",
                e
            )
        }

        return result
    }

    private fun savePendingNotifications(
        notifications: List<String>
    ) {

        try {

            val json =
                JSONArray()

            notifications.forEach {
                name ->

                if (
                    name.trim().isNotEmpty()
                ) {

                    json.put(
                        name.trim()
                    )
                }
            }

            getSharedPreferences(
                notificationPrefsName,
                MODE_PRIVATE
            )
                .edit()
                .putString(
                    pendingNotificationsKey,
                    json.toString()
                )
                .apply()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Pending notification save error",
                e
            )
        }
    }

    private fun showNotificationDialog() {

        if (isAndroidTV) return

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val pendingNotifications =
            getPendingNotifications()

        if (
            pendingNotifications.isEmpty()
        ) {

            Toast.makeText(
                this,
                "কোনো নতুন Notification নেই",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val notificationItems =
            pendingNotifications
                .map {
                    "🔴  নতুন Channel যুক্ত হয়েছে\n$it"
                }
                .toTypedArray()

        try {

            AlertDialog.Builder(this)
                .setTitle("Notification")
                .setItems(
                    notificationItems
                ) {
                    dialog,
                    which ->

                    if (
                        which < 0 ||
                        which >= pendingNotifications.size
                    ) {
                        return@setItems
                    }

                    val channelName =
                        pendingNotifications[which]

                    val remainingNotifications =
                        pendingNotifications
                            .toMutableList()

                    remainingNotifications.removeAt(
                        which
                    )

                    savePendingNotifications(
                        remainingNotifications
                    )

                    dialog.dismiss()

                    val channel =
                        channels.firstOrNull {
                            it.name.trim()
                                .equals(
                                    channelName.trim(),
                                    ignoreCase = true
                                )
                        }

                    if (channel == null) {

                        Toast.makeText(
                            this,
                            "Channel পাওয়া যায়নি",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setItems
                    }

                    openChannelFromNotification(
                        channel
                    )
                }
                .setNegativeButton(
                    "বন্ধ করুন",
                    null
                )
                .show()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Notification dialog error",
                e
            )

            Toast.makeText(
                this,
                "Notification খোলা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openChannelFromNotification(
        channel: Channel
    ) {

        if (isAndroidTV) return

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        if (isFullscreen) {
            exitFullscreen()
        }

        if (
            searchBox.visibility ==
            View.VISIBLE
        ) {

            searchBox.visibility =
                View.GONE

            searchBox.text.clear()
        }

        channelAdapter.updateList(
            channels
        )

        channelRecycler.post {
            updateRecyclerHeight()
        }

        playChannel(
            channel,
            false
        )

        mainScrollView.post {

            if (
                isFinishing ||
                isDestroyed
            ) {
                return@post
            }

            channelRecycler.post {

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@post
                }

                val channelIndex =
                    channels.indexOfFirst {
                        it.name.trim()
                            .equals(
                                channel.name.trim(),
                                ignoreCase = true
                            )
                    }

                if (channelIndex < 0) {
                    return@post
                }

                channelRecycler.scrollToPosition(
                    channelIndex
                )

                channelRecycler.post {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@post
                    }

                    val holder =
                        channelRecycler
                            .findViewHolderForAdapterPosition(
                                channelIndex
                            )

                    val itemTop =
                        holder
                            ?.itemView
                            ?.top ?: 0

                    mainScrollView.smoothScrollTo(
                        0,
                        channelRecycler.top +
                                itemTop
                    )
                }
            }
        }
    }

    // ============================================================
    // MOBILE METHODS
    // ============================================================
    //
    // নিচের methods আপনার আগের Mobile code-এর জন্য রাখা হয়েছে।
    // ============================================================

    private fun setupBottomNavigationInsets() {
        /* unchanged from existing mobile code */
    }

    private fun checkForUpdateAutomatically() {
        /* unchanged from existing mobile code */
    }

    private fun openUpdateScreen() {
        try {

            startActivity(
                Intent(
                    this,
                    UpdateActivity::class.java
                )
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to open UpdateActivity",
                e
            )
        }
    }

    private fun initializeViews() {
        /* unchanged from existing mobile code */
    }

    private fun setupChannelList() {
        /* unchanged from existing mobile code */
    }

    private fun updateRecyclerHeight() {
        /* unchanged from existing mobile code */
    }

    private fun setupPlayer() {
        /* unchanged from existing mobile code */
    }

    private fun setupPlayerControls() {
        /* unchanged from existing mobile code */
    }

    private fun updatePlayPauseButton(
        isPlaying: Boolean
    ) {
        /* unchanged from existing mobile code */
    }

    private fun showControlsTemporarily() {
        /* unchanged from existing mobile code */
    }

    private fun playChannel(
        channel: Channel,
        scrollHomeToTop: Boolean = true
    ) {
        /* unchanged from existing mobile code */
    }

    private fun resumePlayback() {
        /* unchanged from existing mobile code */
    }

    private fun setupSearch() {
        /* unchanged from existing mobile code */
    }

    private fun setupFullscreenButton() {
        /* unchanged from existing mobile code */
    }

    private fun enterFullscreen() {
        /* unchanged from existing mobile code */
    }

    private fun applyFullscreenPlayerSize() {
        /* unchanged from existing mobile code */
    }

    private fun exitFullscreen() {
        /* unchanged from existing mobile code */
    }

    private fun restoreNormalLayout() {
        /* unchanged from existing mobile code */
    }

    private fun hideSystemBars() {
        /* unchanged from existing mobile code */
    }

    private fun showSystemBars() {
        /* unchanged from existing mobile code */
    }

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(
            newConfig
        )

        /* unchanged from existing mobile code */
    }

    private fun setupBottomMenu() {
        /* unchanged from existing mobile code */
    }

    private fun setupSocialLinks() {
        /* unchanged from existing mobile code */
    }

    private fun openUrl(
        url: String
    ) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (
            e: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Link খোলা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Open URL error",
                e
            )
        }
    }

    override fun onResume() {
        super.onResume()

        /* unchanged from existing mobile code */
    }

    override fun onPause() {
        super.onPause()

        /* unchanged from existing mobile code */
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        try {
            tvPlayer?.release()
        } catch (_: Exception) {
        }

        tvPlayer = null

        super.onDestroy()

        /* unchanged from existing mobile code */
    }
}
