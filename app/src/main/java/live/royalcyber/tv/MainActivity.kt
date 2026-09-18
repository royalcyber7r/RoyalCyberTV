package live.royalcyber.tv

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import org.json.JSONArray

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
       PLAYER CONTROLS
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
     * TV-এর জন্য আলাদা simple adapter।
     *
     * Mobile ChannelAdapter একদম untouched থাকবে।
     */
    private var tvAdapter: TVSimpleAdapter? = null

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

        super.onCreate(
            savedInstanceState
        )

        /*
         * =====================================================
         * ANDROID TV
         * =====================================================
         *
         * TV-তে শুধু:
         *
         * 1. TV layout
         * 2. Simple channel grid
         * 3. channels.json
         *
         * Mobile-এর কোনো view initialize হবে না।
         * ExoPlayer-ও TV startup-এ চালু হবে না।
         * =====================================================
         */

        if (isAndroidTV) {

            setContentView(
                R.layout.activity_tv_main
            )

            setupSimpleTV()

            loadChannelsFromJson()

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
            updateRecyclerHeight()
        }

        /*
         * Mobile automatic update check।
         *
         * TV এখানে আসবে না।
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
       SIMPLE TV SETUP
       ========================================================= */

    private fun setupSimpleTV() {

        val recycler =
            findViewById<RecyclerView>(
                R.id.tv_channel_recycler
            )

        recycler.layoutManager =
            GridLayoutManager(
                this,
                5
            )



        recycler.isFocusable =
            true

        recycler.isFocusableInTouchMode =
            true

        recycler.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        tvAdapter =
            TVSimpleAdapter(
                emptyList()
            ) { channel ->

                /*
                 * TV এখন শুধু channel select করবে।
                 *
                 * Player পরের ধাপে যোগ করা হবে।
                 */

                currentChannel =
                    channel
            }

        recycler.adapter =
            tvAdapter

        /*
         * প্রথমে RecyclerView focus নেবে।
         */
        recycler.requestFocus()
    }

    /* =========================================================
       SIMPLE TV ADAPTER
       ========================================================= */

    private class TVSimpleAdapter(
        private var list: List<Channel>,
        private val onClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<TVSimpleAdapter.TVHolder>() {

        class TVHolder(
            val textView: TextView
        ) : RecyclerView.ViewHolder(
            textView
        )

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): TVHolder {

            val density =
                parent.resources.displayMetrics.density

            val height =
                (105 * density).toInt()

            val margin =
                (6 * density).toInt()

            val textView =
                TextView(
                    parent.context
                )

            val params =
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height
                )

            params.setMargins(
                margin,
                margin,
                margin,
                margin
            )

            textView.layoutParams =
                params

            textView.gravity =
                Gravity.CENTER

            textView.textSize =
                17f

            textView.setTextColor(
                Color.WHITE
            )

            textView.setPadding(
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt()
            )

            textView.isFocusable =
                true

            textView.isFocusableInTouchMode =
                true

            textView.isClickable =
                true

            setTVBackground(
                textView,
                false
            )

            textView.setOnFocusChangeListener {
                    view,
                    hasFocus ->

                setTVBackground(
                    view as TextView,
                    hasFocus
                )

                if (hasFocus) {

                    view.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(100)
                        .start()

                } else {

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }

            return TVHolder(
                textView
            )
        }

        override fun onBindViewHolder(
            holder: TVHolder,
            position: Int
        ) {

            val channel =
                list[position]

            holder.textView.text =
                channel.name

            holder.textView.setOnClickListener {

                onClick(
                    channel
                )
            }

            setTVBackground(
                holder.textView,
                holder.textView.hasFocus()
            )
        }

        override fun getItemCount(): Int {
            return list.size
        }

        fun update(
            newList: List<Channel>
        ) {

            list =
                newList

            notifyDataSetChanged()
        }

        private fun setTVBackground(
            view: TextView,
            focused: Boolean
        ) {

            val background =
                GradientDrawable()

            background.cornerRadius =
                10f

            if (focused) {

                background.setColor(
                    Color.rgb(
                        45,
                        45,
                        45
                    )
                )

                background.setStroke(
                    3,
                    Color.WHITE
                )

            } else {

                background.setColor(
                    Color.rgb(
                        20,
                        20,
                        20
                    )
                )

                background.setStroke(
                    1,
                    Color.rgb(
                        70,
                        70,
                        70
                    )
                )
            }

            view.background =
                background
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
                        )
                            ?: continue

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

                    /*
                     * =================================================
                     * TV
                     * =================================================
                     */

                    if (isAndroidTV) {

                        tvAdapter?.update(
                            channels
                        )

                        val recycler =
                            findViewById<RecyclerView>(
                                R.id.tv_channel_recycler
                            )

                        recycler.post {

                            if (
                                channels.isNotEmpty()
                            ) {

                                recycler.scrollToPosition(
                                    0
                                )

                                recycler.post {

                                    val holder =
                                        recycler
                                            .findViewHolderForAdapterPosition(
                                                0
                                            )

                                    holder
                                        ?.itemView
                                        ?.requestFocus()

                                    if (
                                        holder == null
                                    ) {

                                        recycler.requestFocus()
                                    }
                                }

                            } else {

                                recycler.requestFocus()
                            }
                        }

                        /*
                         * TV-তে এখানেই শেষ।
                         *
                         * Mobile adapter,
                         * notification,
                         * player—
                         * কিছুই চালানো হবে না।
                         */

                        return@runOnUiThread
                    }

                    /*
                     * =================================================
                     * MOBILE
                     * =================================================
                     */

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

                    /*
                     * Existing notification system।
                     */

                    syncNewChannelNotifications()

                    /*
                     * Mobile:
                     * আগের মতো first channel auto-play।
                     */

                    if (
                        channels.isNotEmpty() &&
                        currentChannel == null
                    ) {

                        playChannel(
                            channels[0]
                        )
                    }
                }

            } catch (
                _: Exception
            ) {

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
            _: Exception
        ) {
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
            _: Exception
        ) {
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

        } catch (
            _: Exception
        ) {
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
                    "🔴  নতুন Channel যুক্ত হয়েছে\n${it}"
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

        } catch (
            _: Exception
        ) {

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

            currentChannel =
                channel

            val recycler =
                findViewById<RecyclerView>(
                    R.id.tv_channel_recycler
                )

            recycler.post {

                val index =
                    channels.indexOfFirst {

                        it.name.trim()
                            .equals(
                                channel.name.trim(),
                                ignoreCase = true
                            )
                    }

                if (index >= 0) {

                    recycler.scrollToPosition(
                        index
                    )

                    recycler.post {

                        recycler
                            .findViewHolderForAdapterPosition(
                                index
                            )
                            ?.itemView
                            ?.requestFocus()
                    }
                }
            }

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

                if (
                    channelIndex < 0
                ) {
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

                    holder
                        ?.itemView
                        ?.requestFocus()
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

        } catch (
            _: Exception
        ) {

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

        } catch (
            _: Exception
        ) {

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

        val columns =
            3

        channelRecycler.layoutManager =
            GridLayoutManager(
                this,
                columns
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

    /* =========================================================
       RECYCLER HEIGHT
       ========================================================= */

    private fun updateRecyclerHeight() {

        if (
            !::channelAdapter.isInitialized
        ) {
            return
        }

        if (
            !::channelRecycler.isInitialized
        ) {
            return
        }

        val itemCount =
            channelAdapter.itemCount

        val columns =
            3

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
       PLAYER
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
            _: Exception
        ) {

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
       PLAY CHANNEL
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

        if (
            exoPlayer == null
        ) {

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
            _: Exception
        ) {

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
       RESUME PLAYBACK
       ========================================================= */

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

                    playChannel(
                        it
                    )

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
                                    .contains(
                                        query
                                    )
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

        if (
            isFullscreen
        ) {
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

        if (
            !isFullscreen
        ) {
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

    /* =========================================================
       RESTORE NORMAL LAYOUT
       ========================================================= */

    private fun restoreNormalLayout() {

        if (
            isFullscreen
        ) {
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
                if (
                    searchWasVisible
                ) {
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
         * TV-তে এখন কোনো fullscreen player নেই।
         * তাই configuration change-এ কিছু করার দরকার নেই।
         */

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

                    mainScrollView
                        .isVerticalScrollBarEnabled =
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

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(
                intent
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
         * TV:
         * এখন কোনো Player নেই।
         */
        if (isAndroidTV) {
            return
        }

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

                if (
                    currentChannel != null
                ) {

                    resumePlayback()
                }

            } else if (
                exoPlayer.playbackState ==
                Player.STATE_ENDED
            ) {

                if (
                    currentChannel != null
                ) {

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

        /*
         * TV-তে Player নেই।
         */
        if (!isAndroidTV) {

            try {

                player?.pause()

            } catch (
                _: Exception
            ) {
            }
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
         * TV adapter
         */
        tvAdapter =
            null

        /*
         * Mobile player
         */
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

        player =
            null

        /*
         * Mobile ChannelAdapter
         */
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
