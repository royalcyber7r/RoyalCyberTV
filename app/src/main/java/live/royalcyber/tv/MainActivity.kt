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
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import org.json.JSONArray

@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    /* =========================================================
       MOBILE VIEWS
       ========================================================= */

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

    /* =========================================================
       MOBILE PLAYER CONTROLS
       ========================================================= */

    private lateinit var playerControls: View
    private lateinit var playPauseButton: TextView
    private lateinit var rewindButton: TextView
    private lateinit var forwardButton: TextView
    private lateinit var liveText: TextView

    private var player: ExoPlayer? = null

    /* =========================================================
       ANDROID TV
       ========================================================= */

    private var tvPlayerView: PlayerView? = null
    private var tvChannelRecycler: RecyclerView? = null
    private var tvCurrentChannel: TextView? = null
    private var tvUpdateButton: TextView? = null
    private var tvBottomUpdateButton: TextView? = null

    private var tvPlayer: ExoPlayer? = null
    private var tvChannels: List<Channel> = emptyList()
    private var tvCurrentIndex = -1
    private var tvChannelAdapter: ChannelAdapter? = null

    /* =========================================================
       CURRENT CHANNEL
       ========================================================= */

    private var currentChannel: Channel? = null

    private var isFullscreen = false

    private var normalPlayerHeight = 220

    private var searchWasVisible = false

    /* =========================================================
       BOTTOM NAVIGATION
       ========================================================= */

    private var bottomNavigationBaseHeight = 70

    /* =========================================================
       ANDROID TV DETECTION
       ========================================================= */

    private val isAndroidTV: Boolean
        get() {
            return try {
                packageManager.hasSystemFeature(
                    PackageManager.FEATURE_LEANBACK
                )
            } catch (_: Exception) {
                false
            }
        }

    /* =========================================================
       UPDATE
       ========================================================= */

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
                playerControls.visibility =
                    View.GONE
            }
        }

    /* =========================================================
       NOTIFICATION
       ========================================================= */

    private val notificationPrefsName =
        "royalcyber_notification_prefs"

    private val knownChannelsKey =
        "known_channels"

    private val pendingNotificationsKey =
        "pending_notifications"

    /* =========================================================
       CHANNEL LIST
       ========================================================= */

    private var channels: List<Channel> =
        emptyList()

    /* =========================================================
       ON CREATE
       ========================================================= */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * =====================================================
         * ANDROID TV
         * =====================================================
         */

        if (isAndroidTV) {

            try {

                setContentView(
                    R.layout.activity_tv_main
                )

                setupAndroidTV()

            } catch (e: Exception) {

                /*
                 * TV layout/player-এর কোনো সমস্যা হলে
                 * Activity যেন সরাসরি crash না করে।
                 */

                showTVError(
                    "TV Screen চালু করা যাচ্ছে না"
                )
            }

            return
        }

        /*
         * =====================================================
         * MOBILE
         * =====================================================
         */

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

        /*
         * MOBILE ONLY UPDATE
         */

        handler.postDelayed({

            if (
                !isFinishing &&
                !isDestroyed &&
                !isAndroidTV
            ) {
                checkForUpdateAutomatically()
            }

        }, 1500)
    }

    /* =========================================================
       TV ERROR
       ========================================================= */

    private fun showTVError(
        message: String
    ) {

        try {

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {
        }
    }

    /* =========================================================
       LOAD CHANNELS FROM ONLINE JSON
       ========================================================= */

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

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.useCaches =
                    false

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
                    JSONArray(
                        jsonText
                    )

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
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

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
                        ::channelRecycler.isInitialized
                    ) {

                        channelRecycler.post {
                            updateRecyclerHeight()
                        }
                    }

                    syncNewChannelNotifications()

                    if (
                        channels.isNotEmpty() &&
                        currentChannel == null
                    ) {

                        playChannel(
                            channels[0]
                        )
                    }
                }

            } catch (_: Exception) {

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

    /* =========================================================
       NOTIFICATION SYSTEM
       ========================================================= */

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

                    !knownChannels.contains(
                        it
                    )
                }

            newChannels.forEach { newChannel ->

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

        } catch (_: Exception) {
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

        } catch (_: Exception) {
        }

        return result
    }

    private fun savePendingNotifications(
        notifications: List<String>
    ) {

        try {

            val json =
                JSONArray()

            notifications.forEach { name ->

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

        } catch (_: Exception) {
        }
    }

    /* =========================================================
       SHOW NOTIFICATION
       ========================================================= */

    private fun showNotificationDialog() {

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
                .setTitle(
                    "Notification"
                )
                .setItems(
                    notificationItems
                ) { dialog, which ->

                    if (
                        which < 0 ||
                        which >=
                        pendingNotifications.size
                    ) {
                        return@setItems
                    }

                    val channelName =
                        pendingNotifications[
                            which
                        ]

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

                    if (
                        channel == null
                    ) {

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

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Notification খোলা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /* =========================================================
       OPEN CHANNEL FROM NOTIFICATION
       ========================================================= */

    private fun openChannelFromNotification(
        channel: Channel
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        if (
            isFullscreen
        ) {
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

                if (
                    channelIndex < 0
                ) {
                    return@post
                }

                channelRecycler.scrollToPosition(
                    channelIndex
                )

                channelRecycler.post {

                    val holder =
                        channelRecycler
                            .findViewHolderForAdapterPosition(
                                channelIndex
                            )

                    val itemTop =
                        holder
                            ?.itemView
                            ?.top
                            ?: 0

                    mainScrollView.smoothScrollTo(
                        0,
                        channelRecycler.top +
                            itemTop
                    )
                }
            }
        }
    }

    /* =========================================================
       BOTTOM NAVIGATION INSETS
       ========================================================= */

    private fun setupBottomNavigationInsets() {

        if (
            !::bottomNavigation.isInitialized
        ) {
            return
        }

        val density =
            resources.displayMetrics.density

        bottomNavigationBaseHeight =
            (
                70f * density
            ).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(
            bottomNavigation
        ) { view, insets ->

            val navigationInsets =
                insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
                )

            val bottomInset =
                navigationInsets.bottom

            val params =
                view.layoutParams

            params.height =
                bottomNavigationBaseHeight +
                    bottomInset

            view.layoutParams =
                params

            view.setPadding(
                view.paddingLeft,
                0,
                view.paddingRight,
                bottomInset
            )

            insets
        }

        ViewCompat.requestApplyInsets(
            bottomNavigation
        )
    }

    /* =========================================================
       AUTOMATIC UPDATE
       ========================================================= */

    private fun checkForUpdateAutomatically() {

        if (
            isAndroidTV
        ) {
            return
        }

        if (
            updateCheckStarted
        ) {
            return
        }

        updateCheckStarted =
            true

        try {

            startActivity(
                Intent(
                    this,
                    UpdateActivity::class.java
                )
            )

        } catch (_: Exception) {

            updateCheckStarted =
                false
        }
    }

    /* =========================================================
       MANUAL UPDATE
       ========================================================= */

    private fun openUpdateScreen() {

        try {

            startActivity(
                Intent(
                    this,
                    UpdateActivity::class.java
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Update System চালু করা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /* =========================================================
       INITIALIZE MOBILE VIEWS
       ========================================================= */

    private fun initializeViews() {

        playerView =
            findViewById(
                R.id.player_view
            )

        playerContainer =
            findViewById(
                R.id.player_container
            )

        channelRecycler =
            findViewById(
                R.id.channel_recycler
            )

        searchButton =
            findViewById(
                R.id.search_button
            )

        searchBox =
            findViewById(
                R.id.search_box
            )

        fullscreenButton =
            findViewById(
                R.id.fullscreen_button
            )

        mainScrollView =
            findViewById(
                R.id.main_scroll_view
            )

        headerLayout =
            findViewById(
                R.id.header_layout
            )

        currentChannelName =
            findViewById(
                R.id.current_channel_name
            )

        channelTitle =
            findViewById(
                R.id.channel_title
            )

        footerLayout =
            findViewById(
                R.id.footer_layout
            )

        bottomNavigation =
            findViewById(
                R.id.bottom_navigation
            )

        footerFacebook =
            findViewById(
                R.id.footer_facebook
            )

        footerYoutube =
            findViewById(
                R.id.footer_youtube
            )

        footerInstagram =
            findViewById(
                R.id.footer_instagram
            )

        footerTiktok =
            findViewById(
                R.id.footer_tiktok
            )

        playerControls =
            findViewById(
                R.id.player_controls
            )

        playPauseButton =
            findViewById(
                R.id.play_pause_button
            )

        rewindButton =
            findViewById(
                R.id.rewind_button
            )

        forwardButton =
            findViewById(
                R.id.forward_button
            )

        liveText =
            findViewById(
                R.id.live_text
            )
    }

    /* =========================================================
       MOBILE CHANNEL LIST
       ========================================================= */

    private fun setupChannelList() {

        channelRecycler.layoutManager =
            GridLayoutManager(
                this,
                3
            )

        channelAdapter =
            ChannelAdapter(
                channels = channels,
                onChannelClick = { channel ->

                    playChannel(
                        channel
                    )
                }
            )

        channelRecycler.adapter =
            channelAdapter

        channelRecycler.isNestedScrollingEnabled =
            false

        channelRecycler.setHasFixedSize(
            false
        )

        channelRecycler.isFocusable =
            false

        channelRecycler.isFocusableInTouchMode =
            false

        updateRecyclerHeight()
    }

    private fun updateRecyclerHeight() {

        if (
            !::channelAdapter.isInitialized ||
            !::channelRecycler.isInitialized
        ) {
            return
        }

        val itemCount =
            channelAdapter.itemCount

        val columns =
            if (isAndroidTV) {
                5
            } else {
                3
            }

        val rows =
            if (
                itemCount == 0
            ) {
                0
            } else {
                (
                    itemCount +
                        columns -
                        1
                    ) / columns
            }

        val density =
            resources.displayMetrics.density

        val rowHeightDp =
            145

        val bottomPaddingDp =
            15

        val heightPx =
            (
                rows *
                    rowHeightDp +
                    bottomPaddingDp
                ) * density

        val params =
            channelRecycler.layoutParams

        params.height =
            heightPx.toInt()

        channelRecycler.layoutParams =
            params
    }

    /* =========================================================
       MOBILE PLAYER
       ========================================================= */

    private fun setupPlayer() {

        try {

            player =
                ExoPlayer.Builder(
                    this
                ).build()

            playerView.player =
                player

            playerView.useController =
                false

            playerView.keepScreenOn =
                true

            playerView.setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            player?.repeatMode =
                Player.REPEAT_MODE_ONE

            player?.addListener(
                object : Player.Listener {

                    override fun onIsPlayingChanged(
                        isPlaying: Boolean
                    ) {

                        if (
                            !isFinishing &&
                            !isDestroyed
                        ) {

                            updatePlayPauseButton(
                                isPlaying
                            )
                        }
                    }

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }

                        when (
                            playbackState
                        ) {

                            Player.STATE_BUFFERING -> {

                                liveText.text =
                                    "●  BUFFERING..."
                            }

                            Player.STATE_READY -> {

                                liveText.text =
                                    "●  LIVE"
                            }

                            Player.STATE_ENDED -> {

                                liveText.text =
                                    "●  LIVE"
                            }
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }

                        liveText.text =
                            "●  ERROR"

                        updatePlayPauseButton(
                            false
                        )

                        Toast.makeText(
                            this@MainActivity,
                            "এই Channel চালু করা যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

        } catch (_: Exception) {

            player =
                null

            Toast.makeText(
                this,
                "Player চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /* =========================================================
       PLAYER CONTROLS
       ========================================================= */

    private fun setupPlayerControls() {

        playPauseButton.setOnClickListener {

            val exoPlayer =
                player
                    ?: return@setOnClickListener

            try {

                when {

                    exoPlayer.playbackState ==
                        Player.STATE_IDLE -> {

                        resumePlayback()
                    }

                    exoPlayer.playbackState ==
                        Player.STATE_ENDED -> {

                        resumePlayback()
                    }

                    exoPlayer.isPlaying -> {

                        exoPlayer.pause()
                    }

                    else -> {

                        exoPlayer.play()
                    }
                }

                updatePlayPauseButton(
                    exoPlayer.isPlaying
                )

                showControlsTemporarily()

            } catch (_: Exception) {
            }
        }

        rewindButton.setOnClickListener {

            try {
                player?.seekBack()
            } catch (_: Exception) {
            }

            showControlsTemporarily()
        }

        forwardButton.setOnClickListener {

            try {
                player?.seekForward()
            } catch (_: Exception) {
            }

            showControlsTemporarily()
        }

        playerView.setOnClickListener {

            if (
                playerControls.visibility ==
                View.VISIBLE
            ) {

                playerControls.visibility =
                    View.GONE

                handler.removeCallbacks(
                    hideControlsRunnable
                )

            } else {

                playerControls.visibility =
                    View.VISIBLE

                showControlsTemporarily()
            }
        }

        updatePlayPauseButton(false)
    }

    private fun updatePlayPauseButton(
        isPlaying: Boolean
    ) {

        if (
            !::playPauseButton.isInitialized
        ) {
            return
        }

        playPauseButton.text =
            if (isPlaying) {
                "❚❚"
            } else {
                "▶"
            }
    }

    private fun showControlsTemporarily() {

        handler.removeCallbacks(
            hideControlsRunnable
        )

        if (!isFullscreen) {

            handler.postDelayed(
                hideControlsRunnable,
                5000
            )
        }
    }

    /* =========================================================
       PLAY MOBILE CHANNEL
       ========================================================= */

    private fun playChannel(
        channel: Channel,
        scrollHomeToTop: Boolean = true
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val url =
            channel.streamUrl.trim()

        if (
            url.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val exoPlayer =
            player
                ?: return

        currentChannel =
            channel

        try {

            val dataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(
                        true
                    )

            val mediaSource =
                HlsMediaSource.Factory(
                    dataSourceFactory
                )
                    .createMediaSource(
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

            currentChannelName.text =
                channel.name

            liveText.text =
                "●  BUFFERING..."

            playerControls.visibility =
                View.VISIBLE

            updatePlayPauseButton(true)

            showControlsTemporarily()

            if (
                scrollHomeToTop &&
                !isFullscreen
            ) {

                mainScrollView.post {

                    if (
                        !isFinishing &&
                        !isDestroyed
                    ) {

                        mainScrollView.smoothScrollTo(
                            0,
                            0
                        )
                    }
                }
            }

        } catch (_: Exception) {

            liveText.text =
                "●  ERROR"

            updatePlayPauseButton(false)

            Toast.makeText(
                this,
                "এই Channel চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun resumePlayback() {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val exoPlayer =
            player
                ?: return

        try {

            if (
                exoPlayer.playbackState ==
                    Player.STATE_IDLE ||
                exoPlayer.playbackState ==
                    Player.STATE_ENDED
            ) {

                currentChannel?.let {

                    playChannel(it)

                    return
                }
            }

            exoPlayer.playWhenReady =
                true

            exoPlayer.play()

            updatePlayPauseButton(true)

        } catch (_: Exception) {
        }
    }

    /* =========================================================
       SEARCH
       ========================================================= */

    private fun setupSearch() {

        searchButton.setOnClickListener {

            if (
                searchBox.visibility ==
                View.GONE
            ) {

                searchBox.visibility =
                    View.VISIBLE

                searchBox.requestFocus()

            } else {

                searchBox.visibility =
                    View.GONE

                searchBox.text.clear()
            }
        }

        searchBox.addTextChangedListener(
            object :
                android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val query =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    val result =
                        if (
                            query.isEmpty()
                        ) {
                            channels
                        } else {
                            channels.filter {
                                it.name
                                    .lowercase()
                                    .contains(query)
                            }
                        }

                    channelAdapter.updateList(
                        result
                    )

                    channelRecycler.post {
                        updateRecyclerHeight()
                    }
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }

    /* =========================================================
       FULLSCREEN
       ========================================================= */

    private fun setupFullscreenButton() {

        fullscreenButton.setOnClickListener {

            if (isFullscreen) {
                exitFullscreen()
            } else {
                enterFullscreen()
            }
        }
    }

    private fun enterFullscreen() {

        if (isFullscreen) {
            return
        }

        isFullscreen =
            true

        searchWasVisible =
            searchBox.visibility ==
                View.VISIBLE

        mainScrollView.scrollTo(
            0,
            0
        )

        mainScrollView.isVerticalScrollBarEnabled =
            false

        mainScrollView.overScrollMode =
            View.OVER_SCROLL_NEVER

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        headerLayout.visibility =
            View.GONE

        searchBox.visibility =
            View.GONE

        currentChannelName.visibility =
            View.GONE

        channelTitle.visibility =
            View.GONE

        channelRecycler.visibility =
            View.GONE

        footerLayout.visibility =
            View.GONE

        bottomNavigation.visibility =
            View.GONE

        hideSystemBars()

        playerControls.visibility =
            View.VISIBLE

        handler.removeCallbacks(
            hideControlsRunnable
        )

        window.decorView.post {
            if (isFullscreen) {
                applyFullscreenPlayerSize()
            }
        }

        window.decorView.postDelayed({

            if (isFullscreen) {
                applyFullscreenPlayerSize()
            }

        }, 250)
    }

    private fun applyFullscreenPlayerSize() {

        if (!isFullscreen) {
            return
        }

        val viewportWidth =
            mainScrollView.width

        val viewportHeight =
            mainScrollView.height

        if (
            viewportWidth <= 0 ||
            viewportHeight <= 0
        ) {

            window.decorView.post {

                if (isFullscreen) {
                    applyFullscreenPlayerSize()
                }
            }

            return
        }

        try {

            val params =
                playerContainer.layoutParams

            params.width =
                viewportWidth

            params.height =
                viewportHeight

            playerContainer.layoutParams =
                params

            mainScrollView.scrollTo(
                0,
                0
            )

            playerContainer.requestLayout()

        } catch (_: Exception) {
        }
    }

    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen =
            false

        handler.removeCallbacks(
            hideControlsRunnable
        )

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        showSystemBars()

        window.decorView.postDelayed({

            if (!isFullscreen) {
                restoreNormalLayout()
            }

        }, 250)
    }

    private fun restoreNormalLayout() {

        if (isFullscreen) {
            return
        }

        try {

            val density =
                resources.displayMetrics.density

            val params =
                playerContainer.layoutParams

            params.width =
                ViewGroup.LayoutParams.MATCH_PARENT

            params.height =
                (
                    normalPlayerHeight *
                        density
                    ).toInt()

            playerContainer.layoutParams =
                params

            headerLayout.visibility =
                View.VISIBLE

            currentChannelName.visibility =
                View.VISIBLE

            channelTitle.visibility =
                View.VISIBLE

            channelRecycler.visibility =
                View.VISIBLE

            footerLayout.visibility =
                View.VISIBLE

            bottomNavigation.visibility =
                View.VISIBLE

            searchBox.visibility =
                if (searchWasVisible) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            mainScrollView.isVerticalScrollBarEnabled =
                true

            mainScrollView.overScrollMode =
                View.OVER_SCROLL_NEVER

            mainScrollView.scrollTo(
                0,
                0
            )

            mainScrollView.post {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    updateRecyclerHeight()

                    mainScrollView.scrollTo(
                        0,
                        0
                    )
                }
            }

            ViewCompat.requestApplyInsets(
                bottomNavigation
            )

            playerControls.visibility =
                View.VISIBLE

            showControlsTemporarily()

            playerContainer.requestLayout()

        } catch (_: Exception) {
        }
    }

    /* =========================================================
       SYSTEM BARS
       ========================================================= */

    private fun hideSystemBars() {

        try {

            val controller =
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )

            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )

            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        } catch (_: Exception) {
        }
    }

    private fun showSystemBars() {

        try {

            val controller =
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                )

            controller.show(
                WindowInsetsCompat.Type.systemBars()
            )

            if (
                ::bottomNavigation.isInitialized
            ) {

                ViewCompat.requestApplyInsets(
                    bottomNavigation
                )
            }

        } catch (_: Exception) {
        }
    }

    /* =========================================================
       CONFIGURATION
       ========================================================= */

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {

        super.onConfigurationChanged(
            newConfig
        )

        if (isAndroidTV) {
            return
        }

        window.decorView.post {

            if (isFullscreen) {

                if (
                    newConfig.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE
                ) {

                    hideSystemBars()

                    mainScrollView.isVerticalScrollBarEnabled =
                        false

                    mainScrollView.overScrollMode =
                        View.OVER_SCROLL_NEVER

                    mainScrollView.scrollTo(
                        0,
                        0
                    )

                    applyFullscreenPlayerSize()
                }

            } else {

                showSystemBars()

                restoreNormalLayout()
            }
        }
    }

    /* =========================================================
       BOTTOM MENU
       ========================================================= */

    private fun setupBottomMenu() {

        findViewById<View>(
            R.id.menu_home
        ).setOnClickListener {

            if (isFullscreen) {

                exitFullscreen()

                return@setOnClickListener
            }

            mainScrollView.post {

                mainScrollView.smoothScrollTo(
                    0,
                    0
                )
            }
        }

        findViewById<View>(
            R.id.menu_notification
        ).setOnClickListener {

            showNotificationDialog()
        }

        findViewById<View>(
            R.id.menu_update
        ).setOnClickListener {

            openUpdateScreen()
        }

        findViewById<View>(
            R.id.menu_channel
        ).setOnClickListener {

            if (isFullscreen) {

                exitFullscreen()

                return@setOnClickListener
            }

            channelRecycler.post {

                mainScrollView.smoothScrollTo(
                    0,
                    channelRecycler.top
                )
            }
        }
    }

    /* =========================================================
       SOCIAL LINKS
       ========================================================= */

    private fun setupSocialLinks() {

        footerFacebook.setOnClickListener {

            openUrl(
                "https://www.facebook.com/royalcyber.7r"
            )
        }

        footerYoutube.setOnClickListener {

            openUrl(
                "https://www.youtube.com/@rhmultimedia5712"
            )
        }

        footerInstagram.setOnClickListener {

            openUrl(
                "https://www.instagram.com/crimeworld06266"
            )
        }

        footerTiktok.setOnClickListener {

            Toast.makeText(
                this,
                "TikTok link এখনো সেট করা হয়নি",
                Toast.LENGTH_SHORT
            ).show()
        }
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
            _: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Link খোলা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /* =========================================================
       RESUME
       ========================================================= */

    override fun onResume() {

        super.onResume()

        /*
         * =====================================================
         * TV
         * =====================================================
         */

        if (isAndroidTV) {

            tvPlayer?.let { exoPlayer ->

                try {

                    if (
                        exoPlayer.playbackState ==
                        Player.STATE_READY
                    ) {

                        exoPlayer.play()

                    } else if (
                        exoPlayer.playbackState ==
                        Player.STATE_IDLE &&
                        tvCurrentIndex >= 0 &&
                        tvCurrentIndex <
                        tvChannels.size
                    ) {

                        playTVChannel(
                            tvChannels[tvCurrentIndex]
                        )
                    }

                } catch (_: Exception) {
                }
            }

            return
        }

        /* =====================================================
           MOBILE
           ===================================================== */

        if (isFullscreen) {

            hideSystemBars()

            window.decorView.post {

                if (isFullscreen) {
                    applyFullscreenPlayerSize()
                }
            }

        } else {

            if (
                ::bottomNavigation.isInitialized
            ) {

                ViewCompat.requestApplyInsets(
                    bottomNavigation
                )
            }
        }

        player?.let { exoPlayer ->

            try {

                when (
                    exoPlayer.playbackState
                ) {

                    Player.STATE_IDLE,
                    Player.STATE_ENDED -> {

                        if (
                            currentChannel != null
                        ) {
                            resumePlayback()
                        }
                    }

                    Player.STATE_READY -> {

                        exoPlayer.play()

                        updatePlayPauseButton(
                            true
                        )
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    /* =========================================================
       PAUSE
       ========================================================= */

    override fun onPause() {

        if (isAndroidTV) {

            try {
                tvPlayer?.pause()
            } catch (_: Exception) {
            }

        } else {

            try {
                player?.pause()
            } catch (_: Exception) {
            }
        }

        super.onPause()
    }

    /* =========================================================
       ANDROID TV SETUP
       ========================================================= */

    private fun setupAndroidTV() {

        /*
         * TV layout-এর view আলাদাভাবে নেওয়া হচ্ছে।
         * Mobile-এর কোনো view এখানে ব্যবহার করা হবে না।
         */

        try {

            tvPlayerView =
                findViewById(
                    R.id.tv_player_view
                )

            tvChannelRecycler =
                findViewById(
                    R.id.tv_channel_recycler
                )

            tvCurrentChannel =
                findViewById(
                    R.id.tv_current_channel
                )

            tvUpdateButton =
                findViewById(
                    R.id.tv_update_button
                )

            tvBottomUpdateButton =
                findViewById(
                    R.id.tv_bottom_update
                )

        } catch (e: Exception) {

            showTVError(
                "TV view পাওয়া যাচ্ছে না"
            )

            return
        }

        /*
         * =====================================================
         * TV HEADER UPDATE BUTTON
         * =====================================================
         */

        try {

            tvUpdateButton?.setOnClickListener {

                openUpdateScreen()
            }

        } catch (_: Exception) {
        }

        /*
         * =====================================================
         * TV BOTTOM UPDATE BUTTON
         * =====================================================
         */

        try {

            tvBottomUpdateButton?.setOnClickListener {

                openUpdateScreen()
            }

        } catch (_: Exception) {
        }

        /*
         * =====================================================
         * TV PLAYER
         * =====================================================
         */

        setupTVPlayer()

        /*
         * =====================================================
         * TV CHANNEL LIST
         * =====================================================
         */

        setupTVChannelList()

        /*
         * =====================================================
         * JSON
         * =====================================================
         */

        loadChannelsForTV()

        /*
         * =====================================================
         * FIRST FOCUS
         * =====================================================
         */

        tvChannelRecycler?.post {

            if (
                !isFinishing &&
                !isDestroyed
            ) {

                try {

                    tvChannelRecycler
                        ?.requestFocus()

                } catch (_: Exception) {
                }
            }
        }
    }

    /* =========================================================
       TV PLAYER
       ========================================================= */

    private fun setupTVPlayer() {

        val playerView =
            tvPlayerView
                ?: return

        try {

            tvPlayer =
                ExoPlayer.Builder(
                    this
                ).build()

            playerView.player =
                tvPlayer

            playerView.useController =
                true

            playerView.controllerAutoShow =
                true

            playerView.controllerHideOnTouch =
                true

            playerView.keepScreenOn =
                true

            playerView.setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            tvPlayer?.repeatMode =
                Player.REPEAT_MODE_ONE

            tvPlayer?.addListener(
                object :
                    Player.Listener {

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }

                        val current =
                            if (
                                tvCurrentIndex >= 0 &&
                                tvCurrentIndex <
                                tvChannels.size
                            ) {
                                tvChannels[
                                    tvCurrentIndex
                                ].name
                            } else {
                                ""
                            }

                        when (
                            playbackState
                        ) {

                            Player.STATE_BUFFERING -> {

                                tvCurrentChannel?.text =
                                    if (
                                        current.isNotEmpty()
                                    ) {
                                        "● BUFFERING...  $current"
                                    } else {
                                        "● BUFFERING..."
                                    }
                            }

                            Player.STATE_READY -> {

                                tvCurrentChannel?.text =
                                    if (
                                        current.isNotEmpty()
                                    ) {
                                        "● LIVE  $current"
                                    } else {
                                        "● LIVE"
                                    }
                            }

                            Player.STATE_ENDED -> {

                                tvCurrentChannel?.text =
                                    if (
                                        current.isNotEmpty()
                                    ) {
                                        "● LIVE  $current"
                                    } else {
                                        "● LIVE"
                                    }
                            }

                            Player.STATE_IDLE -> {
                            }
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }

                        val current =
                            if (
                                tvCurrentIndex >= 0 &&
                                tvCurrentIndex <
                                tvChannels.size
                            ) {
                                tvChannels[
                                    tvCurrentIndex
                                ].name
                            } else {
                                "Channel"
                            }

                        tvCurrentChannel?.text =
                            "● ERROR  $current"
                    }
                }
            )

        } catch (e: Exception) {

            tvPlayer =
                null

            showTVError(
                "TV Player চালু করা যাচ্ছে না"
            )
        }
    }

    /* =========================================================
       TV CHANNEL LIST
       ========================================================= */

    private fun setupTVChannelList() {

        val recycler =
            tvChannelRecycler
                ?: return

        try {

            recycler.layoutManager =
                GridLayoutManager(
                    this,
                    5
                )

            tvChannelAdapter =
                ChannelAdapter(
                    channels = emptyList(),
                    onChannelClick = { channel ->

                        playTVChannel(
                            channel
                        )
                    }
                )

            recycler.adapter =
                tvChannelAdapter

            recycler.isFocusable =
                true

            recycler.isFocusableInTouchMode =
                true

            recycler.descendantFocusability =
                ViewGroup.FOCUS_AFTER_DESCENDANTS

            recycler.isNestedScrollingEnabled =
                true

            recycler.setHasFixedSize(
                false
            )

        } catch (e: Exception) {

            showTVError(
                "TV Channel list চালু করা যাচ্ছে না"
            )
        }
    }

    /* =========================================================
       LOAD CHANNELS FOR TV
       ========================================================= */

    private fun loadChannelsForTV() {

        Thread {

            try {

                val url =
                    java.net.URL(
                        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"
                    )

                val connection =
                    url.openConnection()
                        as java.net.HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.useCaches =
                    false

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
                    JSONArray(
                        jsonText
                    )

                val loaded =
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

                        loaded.add(
                            Channel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }

                    tvChannels =
                        loaded

                    tvChannelAdapter?.updateList(
                        tvChannels
                    )

                    /*
                     * প্রথম Channel চালু
                     */

                    if (
                        tvChannels.isNotEmpty() &&
                        tvPlayer != null
                    ) {

                        playTVChannel(
                            tvChannels[0]
                        )
                    }
                }

            } catch (_: Exception) {

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }

                    tvCurrentChannel?.text =
                        "● LIVE  Channels loading failed"

                    Toast.makeText(
                        this,
                        "TV-তে channels.json লোড করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    /* =========================================================
       PLAY TV CHANNEL
       ========================================================= */

    private fun playTVChannel(
        channel: Channel
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val exoPlayer =
            tvPlayer
                ?: return

        val recycler =
            tvChannelRecycler
                ?: return

        val url =
            channel.streamUrl.trim()

        if (
            url.isEmpty()
        ) {

            tvCurrentChannel?.text =
                "● ERROR  Stream URL পাওয়া যায়নি"

            return
        }

        val index =
            tvChannels.indexOfFirst {

                it.name.trim()
                    .equals(
                        channel.name.trim(),
                        ignoreCase = true
                    )
            }

        if (
            index < 0
        ) {
            return
        }

        tvCurrentIndex =
            index

        try {

            val dataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(
                        true
                    )

            val mediaSource =
                HlsMediaSource.Factory(
                    dataSourceFactory
                )
                    .setAllowChunklessPreparation(
                        true
                    )
                    .createMediaSource(
                        MediaItem.fromUri(
                            url
                        )
                    )

            exoPlayer.stop()

            exoPlayer.clearMediaItems()

            exoPlayer.setMediaSource(
                mediaSource
            )

            exoPlayer.prepare()

            exoPlayer.playWhenReady =
                true

            tvCurrentChannel?.text =
                "● LIVE  ${channel.name}"

            /*
             * =================================================
             * SELECTED CHANNEL / FOCUS
             * =================================================
             */

            recycler.post {

                if (
                    isFinishing ||
                    isDestroyed
                ) {
                    return@post
                }

                try {

                    recycler.scrollToPosition(
                        index
                    )

                    recycler.post {

                        val holder =
                            recycler
                                .findViewHolderForAdapterPosition(
                                    index
                                )

                        holder?.itemView?.requestFocus()

                    }

                } catch (_: Exception) {
                }
            }

        } catch (_: Exception) {

            tvCurrentChannel?.text =
                "● ERROR  ${channel.name}"

            Toast.makeText(
                this,
                "এই Channel চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /* =========================================================
       TV NEXT / PREVIOUS
       ========================================================= */

    override fun onKeyDown(
        keyCode: Int,
        event: android.view.KeyEvent?
    ): Boolean {

        if (isAndroidTV) {

            when (keyCode) {

                android.view.KeyEvent.KEYCODE_MEDIA_NEXT,
                android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {

                    playNextTVChannel()

                    return true
                }

                android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {

                    playPreviousTVChannel()

                    return true
                }
            }
        }

        return super.onKeyDown(
            keyCode,
            event
        )
    }

    private fun playNextTVChannel() {

        if (
            tvChannels.isEmpty()
        ) {
            return
        }

        var nextIndex =
            tvCurrentIndex + 1

        if (
            nextIndex >=
            tvChannels.size
        ) {
            nextIndex = 0
        }

        playTVChannel(
            tvChannels[nextIndex]
        )
    }

    private fun playPreviousTVChannel() {

        if (
            tvChannels.isEmpty()
        ) {
            return
        }

        var previousIndex =
            tvCurrentIndex - 1

        if (
            previousIndex < 0
        ) {
            previousIndex =
                tvChannels.size - 1
        }

        playTVChannel(
            tvChannels[previousIndex]
        )
    }

    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        /*
         * =====================================================
         * MOBILE PLAYER
         * =====================================================
         */

        if (
            ::playerView.isInitialized
        ) {

            try {
                playerView.player = null
            } catch (_: Exception) {
            }
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }

        player = null

        /*
         * =====================================================
         * MOBILE ADAPTER
         * =====================================================
         */

        if (
            ::channelAdapter.isInitialized
        ) {

            try {
                channelAdapter.shutdown()
            } catch (_: Exception) {
            }
        }

        /*
         * =====================================================
         * TV PLAYER VIEW
         * =====================================================
         */

        try {
            tvPlayerView?.player = null
        } catch (_: Exception) {
        }

        /*
         * =====================================================
         * TV PLAYER
         * =====================================================
         */

        try {
            tvPlayer?.release()
        } catch (_: Exception) {
        }

        tvPlayer = null

        /*
         * =====================================================
         * TV ADAPTER
         * =====================================================
         */

        try {
            tvChannelAdapter?.shutdown()
        } catch (_: Exception) {
        }

        tvChannelAdapter = null

        tvPlayerView = null
        tvChannelRecycler = null
        tvCurrentChannel = null
        tvUpdateButton = null
        tvBottomUpdateButton = null

        super.onDestroy()
    }
}
