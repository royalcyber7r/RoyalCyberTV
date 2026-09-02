package live.royalcyber.tv

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var searchButton: ImageButton
    private lateinit var searchBox: EditText

    private lateinit var channelAdapter: ChannelAdapter

    private var player: ExoPlayer? = null

    /*
     * আপাতত একটি পরীক্ষামূলক Channel রাখা হয়েছে।
     *
     * পরে এখানে তোমার আসল Channel List
     * Name + Logo + M3U8 URL দিয়ে যুক্ত করব।
     */
    private val channels = listOf(

        Channel(
            name = "RoyalCyber TV",
            logo = "",
            streamUrl =
                "https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8"
        )

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()

        setupChannelList()

        setupSearch()

        setupBottomMenu()

        initializePlayer()

        /*
         * অ্যাপ চালু হলে প্রথম Channel
         * automatically চালু হবে।
         */
        if (channels.isNotEmpty()) {
            playChannel(channels[0])
        }
    }

    // =====================================================
    // FIND VIEWS
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
    }

    // =====================================================
    // CHANNEL LIST
    // =====================================================

    private fun setupChannelList() {

        /*
         * ৩টি Channel এক লাইনে।
         */
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

    private fun initializePlayer() {

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

        val playerInstance =
            ExoPlayer.Builder(this)
                .build()

        player = playerInstance

        playerView.player =
            playerInstance

        /*
         * Player controller চালু।
         *
         * এখান থেকেই Play / Pause
         * কাজ করবে।
         */
        playerView.useController = true

        playerView.keepScreenOn = true

        playerInstance.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    Toast.makeText(
                        this@MainActivity,
                        "ভিডিও চালু করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // =====================================================
    // PLAY CHANNEL
    // =====================================================

    private fun playChannel(channel: Channel) {

        val url = channel.streamUrl

        if (url.isBlank()) {

            Toast.makeText(
                this,
                "এই Channel-এর Stream URL নেই",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

        val hlsMediaSource =
            HlsMediaSource.Factory(
                httpDataSourceFactory
            ).createMediaSource(
                MediaItem.fromUri(url)
            )

        player?.apply {

            /*
             * আগের Channel বন্ধ করে
             * নতুন Channel সেট করা হচ্ছে।
             */
            stop()

            clearMediaItems()

            setMediaSource(hlsMediaSource)

            prepare()

            playWhenReady = true

            play()
        }

        /*
         * উপরের Current Channel Name পরিবর্তন।
         */
        findViewById<android.widget.TextView>(
            R.id.current_channel_name
        ).text = channel.name
    }

    // =====================================================
    // SEARCH
    // =====================================================

    private fun setupSearch() {

        /*
         * Search icon চাপলে Search Box
         * Show / Hide হবে।
         */
        searchButton.setOnClickListener {

            if (searchBox.visibility == View.GONE) {

                searchBox.visibility =
                    View.VISIBLE

                searchBox.requestFocus()

            } else {

                searchBox.visibility =
                    View.GONE

                searchBox.text.clear()
            }
        }

        /*
         * Search লিখলে Channel List filter হবে।
         */
        searchBox.addTextChangedListener(
            object : TextWatcher {

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

                    val filteredChannels =
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
                        filteredChannels
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    // =====================================================
    // BOTTOM MENU
    // =====================================================

    private fun setupBottomMenu() {

        /*
         * Home
         */
        findViewById<View>(
            R.id.menu_home
        ).setOnClickListener {

            val scrollView =
                findViewById<android.widget.ScrollView>(
                    android.R.id.content
                )

            /*
             * Home button চাপলে উপরের দিকে
             * যাওয়ার জন্য player area-তে scroll।
             */
            channelRecycler.rootView
                .parent

            Toast.makeText(
                this,
                "Home",
                Toast.LENGTH_SHORT
            ).show()
        }

        /*
         * Notification
         */
        findViewById<View>(
            R.id.menu_notification
        ).setOnClickListener {

            Toast.makeText(
                this,
                "কোনো নতুন Notification নেই",
                Toast.LENGTH_SHORT
            ).show()
        }

        /*
         * Update
         */
        findViewById<View>(
            R.id.menu_update
        ).setOnClickListener {

            Toast.makeText(
                this,
                "আপনার App সর্বশেষ Version-এ আছে",
                Toast.LENGTH_SHORT
            ).show()
        }

        /*
         * Channel
         */
        findViewById<View>(
            R.id.menu_channel
        ).setOnClickListener {

            channelRecycler.requestFocus()

            channelRecycler.smoothScrollToPosition(
                0
            )
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

        super.onDestroy()

        player?.release()

        player = null
    }
}
