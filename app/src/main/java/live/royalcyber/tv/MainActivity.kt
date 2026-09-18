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

    private var currentChannel: Channel? = null

    private var isFullscreen = false

    private var normalPlayerHeight = 220

    private var searchWasVisible = false

    /* =========================================================
       BOTTOM NAVIGATION
       ========================================================= */

    private var bottomNavigationBaseHeight = 70

    /* =========================================================
       ANDROID TV
       ========================================================= */

    private val isAndroidTV: Boolean
        get() =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_LEANBACK
            )

    /*
     * TV views
     *
     * এগুলো lateinit রাখা হয়েছে যাতে existing structure
     * পরিবর্তন না হয়।
     *
     * তবে ব্যবহার করার আগে সব জায়গায়
     * ::variable.isInitialized check করা হয়েছে।
     */

    private lateinit var tvChannelRecycler: RecyclerView
    private lateinit var tvUpdateButton: TextView
    private lateinit var tvBottomUpdate: TextView
    private lateinit var tvHeader: View

    private var tvSetupSuccessful = false

    private var tvPlayerView: PlayerView? = null
    private var tvPlayerContainer: FrameLayout? = null
    private var tvPlayer: ExoPlayer? = null

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
                playerControls.visibility = View.GONE
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

    private var channels: List<Channel> = emptyList()

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

                tvSetupSuccessful =
                    setupAndroidTV()

                /*
                 * JSON load হবে শুধুমাত্র TV layout setup
                 * ঠিকমতো complete হওয়ার পর।
                 *
                 * এতে setupAndroidTV() fail করলে
                 * uninitialized RecyclerView থেকে crash হবে না।
                 */

                if (tvSetupSuccessful) {

                    showTvLoading(true)

                    loadChannelsFromJson()

                } else {

                    showTvLoading(false)

                    Toast.makeText(
                        this,
                        "TV interface প্রস্তুত করা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "TV onCreate error",
                    e
                )

                Toast.makeText(
                    this,
                    "TV interface চালু করা যাচ্ছে না",
                    Toast.LENGTH_LONG
                ).show()
            }

            /*
             * TV-তে automatic update screen খুলবে না।
             */

            return
        }

        /* =====================================================
           MOBILE
           ===================================================== */

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
         * Mobile automatic update behaviour
         * আগের মতো রাখা হয়েছে।
         */

        handler.postDelayed({

            if (
                !isFinishing &&
                !isDestroyed
            ) {

                checkForUpdateAutomatically()
            }

        }, 1500)
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

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.useCaches = false

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {

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
                    index in 0 until jsonArray.length()
                ) {

                    val item =
                        jsonArray.optJSONObject(index)
                            ?: continue

                    val name =
                        item.optString("name")
                            .trim()

                    val logo =
                        item.optString("logo")
                            .trim()

                    val streamUrl =
                        item.optString("streamUrl")
                            .trim()

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

                    /* =================================================
                       UPDATE ADAPTER
                       ================================================= */

                    if (
                        ::channelAdapter.isInitialized
                    ) {

                        channelAdapter.updateList(
                            channels
                        )
                    }

                    /* =================================================
                       TV
                       ================================================= */

                    if (isAndroidTV) {

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

                        /* =================================================
                           MOBILE
                           ================================================= */

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

                    /* =================================================
                       NOTIFICATION
                       ================================================= */

                    syncNewChannelNotifications()

                    /* =================================================
                       FIRST CHANNEL
                       =================================================
                       Mobile:
                           প্রথম channel auto play

                       TV:
                           প্রথম channel auto play নয়
                           remote দিয়ে user select করবে
                       ================================================= */

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

                    if (channels.isEmpty()) {

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

                    if (isAndroidTV) {

                        showTvLoading(false)

                        Toast.makeText(
                            this,
                            "অনলাইন channels.json লোড করা যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "অনলাইন channels.json লোড করা যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        }.start()
    }

    /* =========================================================
       TV LOADING
       ========================================================= */

    private fun showTvLoading(
        show: Boolean
    ) {

        if (!isAndroidTV) {
            return
        }

        try {

            val loadingText =
                findViewById<TextView>(
                    R.id.tv_loading_text
                )

            loadingText?.visibility =
                if (show) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "TV loading view error",
                e
            )
        }
    }

    /* =========================================================
       ANDROID TV SETUP
       ========================================================= */

    private fun setupAndroidTV(): Boolean {

        return try {

            /*
             * =================================================
             * FIND TV VIEWS
             * =================================================
             */

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

            /*
             * Explicit null validation
             *
             * XML-এ কোনো ID missing হলে এখানেই ধরা পড়বে।
             */

            if (
                header == null ||
                recycler == null ||
                updateButton == null ||
                bottomUpdate == null
            ) {

                Log.e(
                    TAG,
                    "One or more TV views are missing from activity_tv_main.xml"
                )

                return false
            }

            /*
             * lateinit assign
             */

            tvHeader =
                header

            tvChannelRecycler =
                recycler

            tvUpdateButton =
                updateButton

            tvBottomUpdate =
                bottomUpdate

            /*
             * =================================================
             * UPDATE BUTTONS
             * ================================================= */

            tvUpdateButton.setOnClickListener {

                openUpdateScreen()
            }

            tvBottomUpdate.setOnClickListener {

                openUpdateScreen()
            }

            /*
             * =================================================
             * TV CHANNEL GRID
             * ================================================= */

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

            /*
             * =================================================
             * CHANNEL ADAPTER
             * ================================================= */

            channelAdapter =
                ChannelAdapter(
                    channels = channels,
                    onChannelClick = { channel ->

                        playTvChannel(
                            channel
                        )
                    }
                )

            tvChannelRecycler.adapter =
                channelAdapter

            /*
             * =================================================
             * INITIAL TV FOCUS
             * ================================================= */

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

    /* =========================================================
       ANDROID TV PLAYER SETUP
       ========================================================= */

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

            /*
             * =================================================
             * EXOPLAYER
             * ================================================= */

            tvPlayer =
                ExoPlayer.Builder(this)
                    .build()

            /*
             * =================================================
             * PLAYER CONTAINER
             * ================================================= */

            tvPlayerContainer =
                FrameLayout(this)

            tvPlayerContainer?.layoutParams =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

            /*
             * =================================================
             * PLAYERVIEW
             * ================================================= */

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

            /*
             * শুরুতে player hidden থাকবে।
             */

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

                tvPlayerView?.player =
                    null

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

    /* =========================================================
       PLAY TV CHANNEL
       ========================================================= */

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

            /*
             * TV player তৈরি করা
             */

            if (tvPlayer == null) {

                val created =
                    setupTvPlayer()

                if (!created) {
                    return
                }
            }

            val exoPlayer =
                tvPlayer
                    ?: return

            /*
             * =================================================
             * HLS DATA SOURCE
             * ================================================= */

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

            /*
             * =================================================
             * PLAYBACK
             * ================================================= */

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

            /*
             * =================================================
             * CHANNEL GRID HIDE
             * ================================================= */

            if (
                ::tvChannelRecycler.isInitialized
            ) {

                tvChannelRecycler.visibility =
                    View.GONE
            }

            /*
             * =================================================
             * PLAYER SHOW
             * ================================================= */

            tvPlayerContainer?.visibility =
                View.VISIBLE

            /*
             * =================================================
             * PLAYER FOCUS
             * ================================================= */

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

    /* =========================================================
       CLOSE TV PLAYER
       ========================================================= */

    private fun closeTvPlayer() {

        if (!isAndroidTV) {
            return
        }

        try {

            tvPlayer?.pause()

        } catch (
            _: Exception
        ) {
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

    /* =========================================================
       TV BACK BUTTON
       ========================================================= */

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

            if (savedKnownChannels == null) {

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

        } catch (
            e: Exception
        ) {

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

            if (raw.isNullOrBlank()) {
                return result
            }

            val json =
                JSONArray(raw)

            for (
                index in 0 until json.length()
            ) {

                val name =
                    json.optString(index)
                        .trim()

                if (
                    name.isNotEmpty() &&
                    !result.contains(name)
                ) {

                    result.add(name)
                }
            }

        } catch (
            e: Exception
        ) {

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

            notifications.forEach { name ->

                if (name.trim().isNotEmpty()) {

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

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Pending notification save error",
                e
            )
        }
    }

    /* =========================================================
       SHOW NOTIFICATION
       ========================================================= */

    private fun showNotificationDialog() {

        if (isAndroidTV) {
            return
        }

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        val pendingNotifications =
            getPendingNotifications()

        if (pendingNotifications.isEmpty()) {

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

        } catch (
            e: Exception
        ) {

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

    /* =========================================================
       OPEN CHANNEL FROM NOTIFICATION
       ========================================================= */

    private fun openChannelFromNotification(
        channel: Channel
    ) {

        if (isAndroidTV) {
            return
        }

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

        if (!::bottomNavigation.isInitialized) {
            return
        }

        val density =
            resources.displayMetrics.density

        bottomNavigationBaseHeight =
            (70f * density).toInt()

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

        if (isAndroidTV) {
            return
        }

        if (updateCheckStarted) {
            return
        }

        updateCheckStarted = true

        try {

            startActivity(
                Intent(
                    this,
                    UpdateActivity::class.java
                )
            )

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Automatic update screen error",
                e
            )

            updateCheckStarted = false
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

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Manual update screen error",
                e
            )

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
            findViewById(R.id.player_view)

        playerContainer =
            findViewById(R.id.player_container)

        channelRecycler =
            findViewById(R.id.channel_recycler)

        searchButton =
            findViewById(R.id.search_button)

        searchBox =
            findViewById(R.id.search_box)

        fullscreenButton =
            findViewById(R.id.fullscreen_button)

        mainScrollView =
            findViewById(R.id.main_scroll_view)

        headerLayout =
            findViewById(R.id.header_layout)

        currentChannelName =
            findViewById(R.id.current_channel_name)

        channelTitle =
            findViewById(R.id.channel_title)

        footerLayout =
            findViewById(R.id.footer_layout)

        bottomNavigation =
            findViewById(R.id.bottom_navigation)

        footerFacebook =
            findViewById(R.id.footer_facebook)

        footerYoutube =
            findViewById(R.id.footer_youtube)

        footerInstagram =
            findViewById(R.id.footer_instagram)

        footerTiktok =
            findViewById(R.id.footer_tiktok)

        playerControls =
            findViewById(R.id.player_controls)

        playPauseButton =
            findViewById(R.id.play_pause_button)

        rewindButton =
            findViewById(R.id.rewind_button)

        forwardButton =
            findViewById(R.id.forward_button)

        liveText =
            findViewById(R.id.live_text)
    }

    /* =========================================================
       MOBILE CHANNEL LIST
       ========================================================= */

    private fun setupChannelList() {

        if (isAndroidTV) {
            return
        }

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

        if (isAndroidTV) {
            return
        }

        if (!::channelAdapter.isInitialized) {
            return
        }

        if (!::channelRecycler.isInitialized) {
            return
        }

        val itemCount =
            channelAdapter.itemCount

        val columns = 3

        val rows =
            if (itemCount == 0) {

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
                ExoPlayer.Builder(this)
                    .build()

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

                        when (playbackState) {

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

                        Log.e(
                            TAG,
                            "Mobile player error",
                            error
                        )

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

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Mobile player setup error",
                e
            )

            player = null

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

            } catch (
                _: Exception
            ) {
            }
        }

        rewindButton.setOnClickListener {

            try {

                player?.seekBack()

            } catch (
                _: Exception
            ) {
            }

            showControlsTemporarily()
        }

        forwardButton.setOnClickListener {

            try {

                player?.seekForward()

            } catch (
                _: Exception
            ) {
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

        updatePlayPauseButton(
            false
        )
    }

    /* =========================================================
       PLAY / PAUSE BUTTON
       ========================================================= */

    private fun updatePlayPauseButton(
        isPlaying: Boolean
    ) {

        if (!::playPauseButton.isInitialized) {
            return
        }

        playPauseButton.text =
            if (isPlaying) {
                "❚❚"
            } else {
                "▶"
            }
    }

    /* =========================================================
       CONTROL AUTO HIDE
       ========================================================= */

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

        if (isAndroidTV) {
            return
        }

        if (
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

        val exoPlayer =
            player

        if (exoPlayer == null) {

            Toast.makeText(
                this,
                "Player প্রস্তুত নয়",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

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

            currentChannelName.text =
                channel.name

            liveText.text =
                "●  BUFFERING..."

            playerControls.visibility =
                View.VISIBLE

            updatePlayPauseButton(
                true
            )

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

        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Mobile channel playback error: ${channel.name}",
                e
            )

            liveText.text =
                "●  ERROR"

            updatePlayPauseButton(
                false
            )

            Toast.makeText(
                this,
                "এই Channel চালু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /* =========================================================
       RESUME MOBILE PLAYBACK
       ========================================================= */

    private fun resumePlayback() {

        if (
            isAndroidTV ||
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

            updatePlayPauseButton(
                true
            )

        } catch (
            _: Exception
        ) {
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
            object : android.text.TextWatcher {

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
                        if (query.isEmpty()) {

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
       FULLSCREEN BUTTON
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

    /* =========================================================
       ENTER FULLSCREEN
       ========================================================= */

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

    /* =========================================================
       FULLSCREEN SIZE
       ========================================================= */

    private fun applyFullscreenPlayerSize() {

        if (!isFullscreen) {
            return
        }

        if (
            !::mainScrollView.isInitialized ||
            !::playerContainer.isInitialized
        ) {
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

        } catch (
            _: Exception
        ) {
        }
    }

    /* =========================================================
       EXIT FULLSCREEN
       ========================================================= */

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
            if (isAndroidTV) {

                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            } else {

                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        showSystemBars()

        window.decorView.postDelayed({

            if (!isFullscreen) {
                restoreNormalLayout()
            }

        }, 250)
    }

    /* =========================================================
       RESTORE NORMAL MOBILE LAYOUT
       ========================================================= */

    private fun restoreNormalLayout() {

        if (isAndroidTV) {
            return
        }

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

        } catch (
            _: Exception
        ) {
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

        } catch (
            _: Exception
        ) {
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

        } catch (
            _: Exception
        ) {
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

        /*
         * TV
         */

        if (isAndroidTV) {

            if (isFullscreen) {
                hideSystemBars()
            } else {
                showSystemBars()
            }

            return
        }

        /*
         * MOBILE
         */

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
       MOBILE BOTTOM MENU
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

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(intent)

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
         * ===================================================== */

        if (isAndroidTV) {

            try {

                if (
                    tvPlayer != null &&
                    tvPlayerContainer?.visibility ==
                    View.VISIBLE
                ) {

                    tvPlayer?.playWhenReady =
                        true

                } else if (
                    tvSetupSuccessful &&
                    ::tvChannelRecycler.isInitialized
                ) {

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

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "TV onResume error",
                    e
                )
            }

            return
        }

        /*
         * =====================================================
         * MOBILE
         * ===================================================== */

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

            if (
                exoPlayer.playbackState ==
                Player.STATE_IDLE
            ) {

                if (currentChannel != null) {
                    resumePlayback()
                }

            } else if (
                exoPlayer.playbackState ==
                Player.STATE_ENDED
            ) {

                if (currentChannel != null) {
                    resumePlayback()
                }

            } else if (
                exoPlayer.playbackState ==
                Player.STATE_READY
            ) {

                try {

                    exoPlayer.play()

                    updatePlayPauseButton(
                        true
                    )

                } catch (
                    _: Exception
                ) {
                }
            }
        }
    }

    /* =========================================================
       PAUSE
       ========================================================= */

    override fun onPause() {

        try {

            player?.pause()

        } catch (
            _: Exception
        ) {
        }

        try {

            tvPlayer?.pause()

        } catch (
            _: Exception
        ) {
        }

        super.onPause()
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
         * TV PLAYER RELEASE
         * ===================================================== */

        try {

            tvPlayerView?.player =
                null

        } catch (
            _: Exception
        ) {
        }

        try {

            tvPlayer?.release()

        } catch (
            _: Exception
        ) {
        }

        tvPlayer = null
        tvPlayerView = null
        tvPlayerContainer = null

        /*
         * =====================================================
         * MOBILE PLAYER RELEASE
         * ===================================================== */

        if (
            ::playerView.isInitialized
        ) {

            try {

                playerView.player =
                    null

            } catch (
                _: Exception
            ) {
            }
        }

        try {

            player?.release()

        } catch (
            _: Exception
        ) {
        }

        player = null

        /*
         * =====================================================
         * CHANNEL ADAPTER
         * ===================================================== */

        if (
            ::channelAdapter.isInitialized
        ) {

            try {

                channelAdapter.shutdown()

            } catch (
                _: Exception
            ) {
            }
        }

        super.onDestroy()
    }
}
