package live.royalcyber.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
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


class MainActivity : AppCompatActivity() {

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

    /* ================= PLAYER CONTROLS ================= */

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

    private val handler =
        Handler(Looper.getMainLooper())

    private val hideControlsRunnable =
        Runnable {
            if (!isFullscreen) {
                playerControls.visibility = View.GONE
            }
        }


    /* =========================================================
       CHANNEL LIST
       ========================================================= */

    private val channels = listOf(

        Channel(
            name = "T Sports",
            logo = "https://yt3.googleusercontent.com/IFgAG_o_AdtX4IauErKIzuFGCj0m4QyH81Q1Uq8H-2Si9ul3vmXkLihDUnn6-QI3xiMZech0AQ=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://tvsen5.aynaott.com/TnMn5kZz8aLm/index.m3u8"
        ),

        Channel(
            name = "Sony Aath",
            logo = "https://upload.wikimedia.org/wikipedia/en/6/64/Sony_Aath_Logo.png",
            streamUrl = "https://live20.bozztv.com/giatvplayout7/giatv-209611/index.m3u8"
        ),

        Channel(
            name = "Jamuna TV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQpAd7TpOPf8Jbo71Y9Pke4Y7APhCsQu0sJeV121SbGzErsYmogJaf9SZs&s=10",
            streamUrl = "https://stream.ottplus.live/live/jamuna_tv_abr/index.m3u8"
        ),

        Channel(
            name = "Channel S",
            logo = "https://upload.wikimedia.org/wikipedia/en/a/aa/Channel_S_Bangladesh_Logo.png",
            streamUrl = "https://app.ncare.live/c3VydmVyX8RpbEU9Mi8xNy8yMDE0GIDU6RgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcGVMZEJCTEFWeVN3PTOmdFsaWRtaW51aiPhnPTI2/channels.stream/live-orgin/channels.stream/playlist.m3u8"
        ),

        Channel(
            name = "Ekhon TV",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/c/c5/Ekhon_Logo.svg/1280px-Ekhon_Logo.svg.png",
            streamUrl = "https://tvsen6.aynaott.com/fbgZV3X17hwWcyfZ4pdb/index.m3u8"
        ),

        Channel(
            name = "Nexus TV",
            logo = "https://yt3.googleusercontent.com/acZUkF66bm_ut3x__Ut9lCkfc8UXAR-IvsKEqQk_bEgyceypzynXcdR65e9rVnKdGRWsSRRgOg=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8"
        ),

        Channel(
            name = "NTV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRBajLCVfnTQd8X3xf8XZkLJIzNKdj35CqJww&s",
            streamUrl = "https://tvsen5.aynaott.com/xV4jEKf3D9zc/tracks-v1a1/mono.ts.m3u8"
        ),

        Channel(
            name = "RTV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTuLgXDPQ2Gtl_6XrS_wvA38NE3jrXsY19axu5oqYpiCL4gxGtoRwu3g&s=10",
            streamUrl = "https://app24.jagobd.com.bd/c3VydmVyX8RpbEU9Mi8xNy8yMFDEEHGcfRgzQ6NTAgdEoaeFzbF92YWxIZTO0U0ezN1IzMyfvcEdsEfeDeKiNkVN3PTOmdFseWRtaW51aiPhnPTI2/rtv-sg.stream/index.m3u8"
        ),

        Channel(
            name = "Deepto TV",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/0/00/Logo_of_Deepto_TV.svg/250px-Logo_of_Deepto_TV.svg.png",
            streamUrl = "https://byphdgllyk.gpcdn.net/hls/deeptotv/0_1/index.m3u8"
        ),

        Channel(
            name = "My TV",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSTwzEuhaRG7YKVDoXfXDYcdlvNkShrJje8Em3lzCPghg&s",
            streamUrl = "https://tvsen6.aynaott.com/XMpHaEf0ANBhv8w6NWR7/index.m3u8"
        ),

        Channel(
            name = "Sangeet Bangla HD",
            logo = "https://yt3.googleusercontent.com/FGx9xqm5eU1DZXDk4ZDRQDK9fyhvZ2LR6gKXhZcJeFunvG9SwT8SB01SxbiD3GDL8MqMKxWXHQ=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://cdn-4.pishow.tv/live/1143/master.m3u8"
        ),

        Channel(
            name = "Maasranga TV HD",
            logo = "https://upload.wikimedia.org/wikipedia/en/3/39/Maasranga_Television_Logo.jpg",
            streamUrl = "https://tvsen5.aynaott.com/maasrangatv/index.m3u8"
        ),

        Channel(
            name = "Channel i HD",
            logo = "https://tstatic.akash-go.com/cms-ui/images/custom-content/1740567626692.png",
            streamUrl = "https://tvsen6.aynaott.com/FNHpYvGZ7FkCE10PwTHm/index.m3u8"
        ),

        Channel(
            name = "Desh TV",
            logo = "https://upload.wikimedia.org/wikipedia/commons/2/25/Desh_tv_logo.jpg",
            streamUrl = "https://tvsen6.aynaott.com/ryFkXfd1a4CQ7mMdc820/index.m3u8"
        ),

        Channel(
            name = "Ananda TV",
            logo = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Logo_of_Ananda_TV.svg/1280px-Logo_of_Ananda_TV.svg.png",
            streamUrl = "https://tvsen6.aynaott.com/LeUAm4F1iixYns3s3Non/index.m3u8"
        ),

        Channel(
            name = "Ekushey TV HD",
            logo = "https://s4.gifyu.com/images/image534fa27d7683f33d.png",
            streamUrl = "https://tvsen6.aynaott.com/y4mEVZNAbeNWTbd6Z2Pw/index.m3u8"
        ),

        Channel(
            name = "Asian TV HD",
            logo = "https://assets-prod.services.toffeelive.com/MyK__poBEef-9-uVmf5l/posters/1eadef5b-28e7-4dc2-b42f-c67a3357c9a0.png",
            streamUrl = "https://stream.ottplus.live/live/asian_tv_abr/index.m3u8"
        ),

        Channel(
            name = "Boishakhi TV",
            logo = "https://upload.wikimedia.org/wikipedia/commons/f/f2/Boishakhi_Tv_Logo.png",
            streamUrl = "https://tvsen6.aynaott.com/1d3uG9VCgrR9DRtWZM57/index.m3u8"
        ),

        Channel(
            name = "A Sports",
            logo = "https://upload.wikimedia.org/wikipedia/en/0/0c/A_Sports_Logo.png",
            streamUrl = "https://tvsen6.aynaott.com/zv68oqPDu7MZZwmHhRxt/index.m3u8"
        ),

        Channel(
            name = "Bein Sports Direct",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQkuwIi4KCSdEc0i8OLMZSEhUkAzkd6cWArxA&s",
            streamUrl = "https://1nyaler.streamhostingcdn.top/stream/23/index.m3u8"
        ),

        Channel(
            name = "Zee 24 Ghanta HD",
            logo = "https://i.postimg.cc/tTNPLBMs/24-Ghanta.jpg",
            streamUrl = "https://d2dsoyvkr33m05.cloudfront.net/index_1.m3u8"
        ),

        Channel(
            name = "ATN News HD",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR_PzLYrDkO7qND7MCx4Tk_awS9J9PwOzcH6Q&s=",
            streamUrl = "https://tvsen6.aynaott.com/da6WMXAk/index.m3u8"
        ),

        Channel(
            name = "DeshBidesh",
            logo = "https://i.imgur.com/Ek1Ohj6.png",
            streamUrl = "https://dbcanada.sonarbanglatv.com/deshebideshe/dbtv/index.m3u8"
        ),

        Channel(
            name = "Goldmines Movies HD",
            logo = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRUso-0xopQb02-1w2nw5xxHpydkdNNFS5Cqg&s",
            streamUrl = "https://cdn-2.pishow.tv/live/1461/master.m3u8"
        ),

        Channel(
            name = "9XM",
            logo = "https://yt3.googleusercontent.com/a5SfL3aMu1G_MufbVoDv0wPz4gcyn_pYtsR3BAMH08B368gh-ytxzBWgPpdmKyWemwCGF0Ql=s900-c-k-c0x00ffffff-no-rj",
            streamUrl = "https://9xjio.wiseplayout.com/9XM/master.m3u8"
        ),

        Channel(
            name = "Yrf Music HD",
            logo = "https://jiotvimages.cdn.jio.com/dare_images/images/channel/756c50edae8599fb760cbbfb22010a75.png",
            streamUrl = "https://cdn-uw2-prod.tsv2.amagi.tv/linear/amg01412-xiaomiasia-yrfmusic-xiaomi/playlist.m3u8"
        ),

        Channel(
            name = "Music India HD",
            logo = "https://static.wikia.nocookie.net/logopedia/images/2/2f/Music_India.jpeg",
            streamUrl = "https://cdn-2.pishow.tv/live/226/master.m3u8"
        ),

        Channel(
            name = "Ekator TV HD",
            logo = "https://s4.gifyu.com/images/imagea02f4314e761661d.png",
            streamUrl = "https://tvsen6.aynaott.com/EWDrV5QskgarZEUBb3pU/index.m3u8"
        ),

        Channel(
            name = "Channel 24 HD",
            logo = "https://upload.wikimedia.org/wikipedia/en/thumb/9/9b/Logo_of_Channel_24_%28Bangladesh%29.svg/1280px-Logo_of_Channel_24_%28Bangladesh%29.svg.png",
            streamUrl = "https://stream.ottplus.live/live/channel_24_abr/index.m3u8"
        ),

        Channel(
            name = "Channel 1 4K",
            logo = "https://www.thedailystar.net/sites/default/files/styles/big_1/public/images/2025/02/24/channel_1.png",
            streamUrl = "https://stream.ottplus.live/live/channel_1_hd_abr/index.m3u8"
        )
    )


    /* =========================================================
       ON CREATE
       ========================================================= */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        initializeViews()

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

        if (channels.isNotEmpty()) {
            playChannel(channels[0])
        }
    }


    /* =========================================================
       INITIALIZE
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
       CHANNEL LIST
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
                    playChannel(channel)
                }
            )

        channelRecycler.adapter =
            channelAdapter

        channelRecycler.isNestedScrollingEnabled =
            false

        channelRecycler.setHasFixedSize(
            false
        )

        updateRecyclerHeight()
    }


    private fun updateRecyclerHeight() {

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
                (itemCount + columns - 1) / columns
            }

        val density =
            resources.displayMetrics.density

        val rowHeightDp = 145

        val bottomPaddingDp = 15

        val heightPx =
            (
                rows * rowHeightDp +
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
       PLAYER SETUP
       ========================================================= */

    private fun setupPlayer() {

        player =
            ExoPlayer.Builder(this)
                .build()

        playerView.player =
            player

        /*
         * Default Media3 controller বন্ধ।
         * আমাদের Custom YouTube-style controller ব্যবহার হবে।
         */
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

                    updatePlayPauseButton(
                        isPlaying
                    )
                }


                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

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

                            resumePlayback()
                        }


                        Player.STATE_IDLE -> {
                        }
                    }
                }


                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "এই Channel চালু করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }


    /* =========================================================
       PLAYER CONTROLS
       ========================================================= */

    private fun setupPlayerControls() {

        /*
         * CENTER PLAY / PAUSE
         */

        playPauseButton.setOnClickListener {

            val exoPlayer =
                player ?: return@setOnClickListener

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
        }


        /*
         * 10 SECOND BACK
         */

        rewindButton.setOnClickListener {

            val exoPlayer =
                player ?: return@setOnClickListener

            exoPlayer.seekBack()

            showControlsTemporarily()
        }


        /*
         * 10 SECOND FORWARD
         */

        forwardButton.setOnClickListener {

            val exoPlayer =
                player ?: return@setOnClickListener

            exoPlayer.seekForward()

            showControlsTemporarily()
        }


        /*
         * VIDEO TAP
         */

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


    /* =========================================================
       PLAY / PAUSE ICON
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
       PLAY CHANNEL
       ========================================================= */

    private fun playChannel(
        channel: Channel
    ) {

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

        currentChannel =
            channel

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

        player?.apply {

            stop()

            clearMediaItems()

            setMediaSource(
                mediaSource
            )

            prepare()

            playWhenReady =
                true

            play()
        }

        currentChannelName.text =
            channel.name

        liveText.text =
            "●  LIVE"

        playerControls.visibility =
            View.VISIBLE

        updatePlayPauseButton(true)

        showControlsTemporarily()

        if (!isFullscreen) {

            mainScrollView.post {

                mainScrollView.smoothScrollTo(
                    0,
                    0
                )
            }
        }
    }


    /* =========================================================
       RESUME
       ========================================================= */

    private fun resumePlayback() {

        val exoPlayer =
            player ?: return

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

        window.decorView.post {

            applyFullscreenPlayerSize()
        }

        playerControls.visibility =
            View.VISIBLE

        handler.removeCallbacks(
            hideControlsRunnable
        )
    }


    private fun applyFullscreenPlayerSize() {

        if (!isFullscreen) {
            return
        }

        val displayMetrics =
            resources.displayMetrics

        val screenHeight =
            displayMetrics.heightPixels

        val params =
            playerContainer.layoutParams

        params.width =
            ViewGroup.LayoutParams.MATCH_PARENT

        params.height =
            screenHeight

        playerContainer.layoutParams =
            params
    }


    private fun exitFullscreen() {

        if (!isFullscreen) {
            return
        }

        isFullscreen =
            false

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

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

        showSystemBars()

        mainScrollView.post {

            mainScrollView.scrollTo(
                0,
                0
            )

            updateRecyclerHeight()
        }

        playerControls.visibility =
            View.VISIBLE

        showControlsTemporarily()
    }


    /* =========================================================
       SYSTEM BARS
       ========================================================= */

    private fun hideSystemBars() {

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

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
    }


    private fun showSystemBars() {

        WindowCompat.setDecorFitsSystemWindows(
            window,
            true
        )

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.show(
            WindowInsetsCompat.Type.systemBars()
        )
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

        window.decorView.post {

            if (isFullscreen) {

                hideSystemBars()

                applyFullscreenPlayerSize()

            } else {

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

            Toast.makeText(
                this,
                "কোনো নতুন Notification নেই",
                Toast.LENGTH_SHORT
            ).show()
        }


        findViewById<View>(
            R.id.menu_update
        ).setOnClickListener {

            Toast.makeText(
                this,
                "আপনার App সর্বশেষ Version-এ আছে",
                Toast.LENGTH_SHORT
            ).show()
        }


        findViewById<View>(
            R.id.menu_channel
        ).setOnClickListener {

            channelRecycler.post {

                mainScrollView.smoothScrollTo(
                    0,
                    channelRecycler.top
                )
            }
        }
    }


    /* =========================================================
       SOCIAL
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

        player?.let {

            if (
                it.playbackState ==
                    Player.STATE_IDLE ||
                it.playbackState ==
                    Player.STATE_ENDED
            ) {

                resumePlayback()

            } else {

                it.play()

                updatePlayPauseButton(
                    true
                )
            }
        }
    }


    /* =========================================================
       PAUSE
       ========================================================= */

    override fun onPause() {

        player?.pause()

        super.onPause()
    }


    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        player?.release()

        player = null

        if (
            ::channelAdapter.isInitialized
        ) {

            channelAdapter.shutdown()
        }

        super.onDestroy()
    }
}
