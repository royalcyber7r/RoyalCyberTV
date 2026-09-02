package live.royalcyber.tv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.media3.common.C
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
    private lateinit var channelRecycler: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter

    private lateinit var searchButton: ImageButton
    private lateinit var searchBox: EditText

    private lateinit var qualityButton: ImageButton
    private lateinit var fullscreenButton: ImageButton

    private lateinit var mainScrollView: ScrollView

    private var player: ExoPlayer? = null

    private var isFullscreen = false


    // =====================================================
    // CHANNEL LIST
    // =====================================================

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
            name = "Bangla Vision HD",
            logo = "https://s4.gifyu.com/images/image5c0bfa6b281be803.png",
            streamUrl = "https://stream.ottplus.live/live/bangla_vision_abr/index.m3u8"
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
        )
    )


    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()

        setupChannelList()

        setupSearch()

        setupPlayer()

        setupQualityButton()

        setupFullscreenButton()

        setupBottomMenu()

        if (channels.isNotEmpty()) {

            playChannel(channels[0])

        }
    }


    // =====================================================
    // INITIALIZE VIEWS
    // =====================================================

    private fun initializeViews() {

        playerView =
            findViewById(R.id.player_view)

        channelRecycler =
            findViewById(R.id.channel_recycler)

        searchButton =
            findViewById(R.id.search_button)

        searchBox =
            findViewById(R.id.search_box)

        qualityButton =
            findViewById(R.id.quality_button)

        fullscreenButton =
            findViewById(R.id.fullscreen_button)

        mainScrollView =
            findViewById(R.id.main_scroll_view)
    }


    // =====================================================
    // CHANNEL LIST
    // =====================================================

    private fun setupChannelList() {

        channelRecycler.layoutManager =
            GridLayoutManager(this, 3)

        channelAdapter =
            ChannelAdapter(
                channels = channels,
                onChannelClick = { channel ->

                    playChannel(channel)

                }
            )

        channelRecycler.adapter =
            channelAdapter
    }


    // =====================================================
    // PLAYER
    // =====================================================

    private fun setupPlayer() {

        player =
            ExoPlayer.Builder(this)
                .build()

        playerView.player =
            player

        playerView.useController =
            true

        playerView.keepScreenOn =
            true

        playerView.controllerShowTimeoutMs =
            5000

        player?.addListener(
            object : Player.Listener {

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


    // =====================================================
    // PLAY CHANNEL
    // =====================================================

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

        val dataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

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

        findViewById<TextView>(
            R.id.current_channel_name
        ).text =
            channel.name

        if (!isFullscreen) {

            mainScrollView.smoothScrollTo(
                0,
                0
            )
        }
    }


    // =====================================================
    // SEARCH
    // =====================================================

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
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }


    // =====================================================
    // QUALITY BUTTON
    // =====================================================

    private fun setupQualityButton() {

        qualityButton.setOnClickListener {

            showQualityMenu()

        }
    }


    // =====================================================
    // QUALITY MENU
    // =====================================================

    private fun showQualityMenu() {

        val popup =
            PopupMenu(
                this,
                qualityButton
            )

        popup.menu.add("Auto")

        popup.menu.add("1080p")

        popup.menu.add("720p")

        popup.menu.add("480p")

        popup.menu.add("360p")

        popup.setOnMenuItemClickListener { item ->

            when (
                item.title.toString()
            ) {

                "Auto" -> {

                    setVideoQuality(
                        null
                    )
                }

                "1080p" -> {

                    setVideoQuality(
                        1080
                    )
                }

                "720p" -> {

                    setVideoQuality(
                        720
                    )
                }

                "480p" -> {

                    setVideoQuality(
                        480
                    )
                }

                "360p" -> {

                    setVideoQuality(
                        360
                    )
                }
            }

            true
        }

        popup.show()
    }


    // =====================================================
    // VIDEO QUALITY
    // =====================================================

    private fun setVideoQuality(
        height: Int?
    ) {

        val exoPlayer =
            player ?: return

        if (height == null) {

            exoPlayer.trackSelectionParameters =
                exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(
                        C.TRACK_TYPE_VIDEO
                    )
                    .setMaxVideoSize(
                        Int.MAX_VALUE,
                        Int.MAX_VALUE
                    )
                    .build()

            Toast.makeText(
                this,
                "Quality: Auto",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        exoPlayer.trackSelectionParameters =
            exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(
                    Int.MAX_VALUE,
                    height
                )
                .build()

        Toast.makeText(
            this,
            "Quality: ${height}p",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =====================================================
    // FULLSCREEN BUTTON
    // =====================================================

    private fun setupFullscreenButton() {

        fullscreenButton.setOnClickListener {

            if (isFullscreen) {

                exitFullscreen()

            } else {

                enterFullscreen()
            }
        }
    }


    // =====================================================
    // ENTER FULLSCREEN
    // =====================================================

    private fun enterFullscreen() {

        isFullscreen =
            true

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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

        fullscreenButton.setImageResource(
            android.R.drawable.ic_menu_revert
        )

        Toast.makeText(
            this,
            "Fullscreen Mode",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =====================================================
    // EXIT FULLSCREEN
    // =====================================================

    private fun exitFullscreen() {

        isFullscreen =
            false

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.show(
            WindowInsetsCompat.Type.systemBars()
        )

        fullscreenButton.setImageResource(
            android.R.drawable.ic_menu_crop
        )

        Toast.makeText(
            this,
            "Fullscreen বন্ধ হয়েছে",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =====================================================
    // BOTTOM MENU
    // =====================================================

    private fun setupBottomMenu() {

        // HOME

        findViewById<View>(
            R.id.menu_home
        ).setOnClickListener {

            mainScrollView.smoothScrollTo(
                0,
                0
            )
        }


        // NOTIFICATION

        findViewById<View>(
            R.id.menu_notification
        ).setOnClickListener {

            Toast.makeText(
                this,
                "কোনো নতুন Notification নেই",
                Toast.LENGTH_SHORT
            ).show()
        }


        // UPDATE

        findViewById<View>(
            R.id.menu_update
        ).setOnClickListener {

            Toast.makeText(
                this,
                "আপনার App সর্বশেষ Version-এ আছে",
                Toast.LENGTH_SHORT
            ).show()
        }


        // CHANNEL

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


    // =====================================================
    // RESUME
    // =====================================================

    override fun onResume() {

        super.onResume()

        player?.play()
    }


    // =====================================================
    // PAUSE
    // =====================================================

    override fun onPause() {

        super.onPause()

        player?.pause()
    }


    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {

        player?.release()

        player =
            null

        super.onDestroy()
    }
}
