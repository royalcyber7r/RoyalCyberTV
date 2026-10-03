package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

class TvPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var channelName: TextView

    private var channelNames =
        ArrayList<String>()

    private var channelUrls =
        ArrayList<String>()

    private var channelLogos =
        ArrayList<String>()

    private var currentIndex = 0

    /*
     * একই channel-এ fallback playback
     * একবারের বেশি হবে না।
     */
    private var fallbackTried = false

    /*
     * Channel change-এর সময়
     * পুরনো retry বন্ধ রাখার জন্য।
     */
    private var playbackGeneration = 0


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * TV screen সবসময় awake রাখবে
         */
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(
            R.layout.activity_tv_player
        )

        playerView =
            findViewById(
                R.id.tv_player
            )

        channelName =
            findViewById(
                R.id.tv_player_name
            )

        /*
         * Channel list গ্রহণ
         */
        channelNames =
            intent.getStringArrayListExtra(
                "channel_names"
            ) ?: ArrayList()

        channelUrls =
            intent.getStringArrayListExtra(
                "channel_urls"
            ) ?: ArrayList()

        channelLogos =
            intent.getStringArrayListExtra(
                "channel_logos"
            ) ?: ArrayList()

        currentIndex =
            intent.getIntExtra(
                "channel_index",
                0
            )

        /*
         * Safety check
         */
        if (
            channelNames.isEmpty() ||
            channelUrls.isEmpty()
        ) {

            val name =
                intent.getStringExtra(
                    "channel_name"
                ) ?: "RoyalCyber TV"

            val streamUrl =
                intent.getStringExtra(
                    "channel_url"
                ) ?: ""

            channelName.text = name

            if (streamUrl.isNotEmpty()) {

                fallbackTried = false

                playChannel(
                    streamUrl,
                    false
                )
            }

            return
        }

        /*
         * Index ঠিক রাখা
         */
        if (
            currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            currentIndex = 0
        }

        /*
         * প্রথম channel play
         */
        playCurrentChannel()
    }


    /**
     * বর্তমানে selected channel play
     */
    private fun playCurrentChannel() {

        if (
            currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            return
        }

        val name =
            if (
                currentIndex <
                    channelNames.size
            ) {
                channelNames[currentIndex]
            } else {
                "RoyalCyber TV"
            }

        val streamUrl =
            channelUrls[currentIndex]

        channelName.text =
            name

        if (streamUrl.isBlank()) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * নতুন channel হলে
         * fallback আবার allow করা হবে।
         */
        fallbackTried = false

        playbackGeneration++

        playChannel(
            streamUrl,
            false
        )
    }


    /**
     * Next Channel
     */
    private fun nextChannel() {

        if (channelUrls.isEmpty()) {
            return
        }

        currentIndex++

        if (
            currentIndex >=
                channelUrls.size
        ) {
            currentIndex = 0
        }

        playCurrentChannel()
    }


    /**
     * Previous Channel
     */
    private fun previousChannel() {

        if (channelUrls.isEmpty()) {
            return
        }

        currentIndex--

        if (currentIndex < 0) {

            currentIndex =
                channelUrls.size - 1
        }

        playCurrentChannel()
    }


    /**
     * TV Player
     *
     * fallbackMode = false
     * প্রথমবার normal HLS configuration
     *
     * fallbackMode = true
     * MIME type force না করে আবার চেষ্টা
     */
    private fun playChannel(
        streamUrl: String,
        fallbackMode: Boolean
    ) {

        /*
         * পুরনো player পুরো release
         */
        player?.release()
        player = null

        /*
         * প্রতিবার নতুন generation
         */
        val myGeneration =
            playbackGeneration

        /*
         * TV-friendly HTTP DataSource
         */
        val httpFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(
                    "Mozilla/5.0 (Linux; Android 7.0; TV) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
                        "Chrome/120.0 Safari/537.36 " +
                        "RoyalCyberTV/1.0"
                )
                .setAllowCrossProtocolRedirects(
                    true
                )

        /*
         * TV-এর জন্য একটু বেশি stable buffer।
         *
         * পুরনো Android TV-তে
         * খুব ছোট buffer দিলে বারবার
         * rebuffer হতে পারে।
         */
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    3000,
                    15000,
                    1000,
                    2500
                )
                .setBackBuffer(
                    0,
                    false
                )
                .build()

        /*
         * MediaSourceFactory
         */
        val mediaSourceFactory =
            DefaultMediaSourceFactory(
                httpFactory
            )

        /*
         * নতুন ExoPlayer
         */
        val newPlayer =
            ExoPlayer.Builder(this)
                .setLoadControl(
                    loadControl
                )
                .setMediaSourceFactory(
                    mediaSourceFactory
                )
                .build()

        player =
            newPlayer

        playerView.player =
            newPlayer

        /*
         * TV live playback
         */
        newPlayer.setVideoScalingMode(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        )

        /*
         * Player listener
         */
        newPlayer.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    android.util.Log.e(
                        "RoyalCyberTV",
                        "Playback Error: " +
                            error.errorCodeName,
                        error
                    )

                    android.util.Log.e(
                        "RoyalCyberTV",
                        "Error message: " +
                            error.message
                    )

                    /*
                     * প্রথম error হলে fallback।
                     *
                     * কিছু server-এর ক্ষেত্রে
                     * MIME type force করলে সমস্যা
                     * হতে পারে।
                     */
                    if (
                        !fallbackMode &&
                        !fallbackTried &&
                        myGeneration ==
                            playbackGeneration
                    ) {

                        fallbackTried = true

                        android.util.Log.w(
                            "RoyalCyberTV",
                            "Trying HLS fallback: " +
                                channelName.text
                        )

                        playChannel(
                            streamUrl,
                            true
                        )

                        return
                    }

                    /*
                     * Fallback-ও ব্যর্থ হলে
                     * কয়েক সেকেন্ড পরে একই stream
                     * আবার prepare করার চেষ্টা।
                     */
                    if (
                        myGeneration ==
                            playbackGeneration
                    ) {

                        android.os.Handler(
                            mainLooper
                        ).postDelayed({

                            if (
                                !isFinishing &&
                                myGeneration ==
                                    playbackGeneration
                            ) {

                                android.util.Log.d(
                                    "RoyalCyberTV",
                                    "Retrying stream: " +
                                        channelName.text
                                )

                                retryCurrentStream(
                                    streamUrl
                                )
                            }

                        }, 3000)
                    }
                }


                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (
                        playbackState
                    ) {

                        Player.STATE_BUFFERING -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "BUFFERING: " +
                                    channelName.text
                            )
                        }

                        Player.STATE_READY -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "READY: " +
                                    channelName.text
                            )
                        }

                        Player.STATE_ENDED -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "ENDED: " +
                                    channelName.text
                            )
                        }
                    }
                }


                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {

                    android.util.Log.d(
                        "RoyalCyberTV",
                        "Playing = " +
                            isPlaying +
                            " | " +
                            channelName.text
                    )
                }
            }
        )


        /*
         * MediaItem
         */
        val mediaItemBuilder =
            MediaItem.Builder()
                .setUri(
                    Uri.parse(streamUrl)
                )

        /*
         * প্রথমবার HLS MIME type force করা।
         *
         * fallback-এ এটি বাদ দেওয়া হবে।
         */
        if (!fallbackMode) {

            mediaItemBuilder.setMimeType(
                "application/x-mpegURL"
            )
        }

        /*
         * Live stream configuration
         */
        mediaItemBuilder.setLiveConfiguration(

            MediaItem.LiveConfiguration.Builder()
                .setMaxPlaybackSpeed(
                    1.02f
                )
                .build()
        )

        val mediaItem =
            mediaItemBuilder.build()


        /*
         * Player-এ stream দেওয়া
         */
        newPlayer.setMediaItem(
            mediaItem
        )

        /*
         * Prepare
         */
        newPlayer.prepare()

        /*
         * Automatically play
         */
        newPlayer.playWhenReady =
            true
    }


    /**
     * Stream আবার চেষ্টা
     */
    private fun retryCurrentStream(
        streamUrl: String
    ) {

        if (isFinishing) {
            return
        }

        /*
         * Retry-তে নতুন player বানানো হবে।
         */
        player?.release()
        player = null

        /*
         * fallback state reset করা হচ্ছে না।
         *
         * এতে error হলে infinite
         * fallback loop হবে না।
         */
        playChannel(
            streamUrl,
            true
        )
    }


    /**
     * Remote Key Handling
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        /*
         * শুধুমাত্র ACTION_UP-এ
         * channel change হবে।
         */
        if (
            event.action ==
                KeyEvent.ACTION_UP
        ) {

            when (
                event.keyCode
            ) {

                /*
                 * Channel Up
                 */
                KeyEvent.KEYCODE_CHANNEL_UP,

                /*
                 * Media Next
                 */
                KeyEvent.KEYCODE_MEDIA_NEXT,

                /*
                 * +
                 */
                KeyEvent.KEYCODE_PLUS -> {

                    nextChannel()

                    return true
                }


                /*
                 * Channel Down
                 */
                KeyEvent.KEYCODE_CHANNEL_DOWN,

                /*
                 * Media Previous
                 */
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,

                /*
                 * -
                 */
                KeyEvent.KEYCODE_MINUS -> {

                    previousChannel()

                    return true
                }


                /*
                 * Back
                 */
                KeyEvent.KEYCODE_BACK -> {

                    finish()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }


    /**
     * Activity background হলে
     * player release
     */
    override fun onStop() {

        super.onStop()

        player?.release()
        player = null
    }


    /**
     * Activity destroy
     */
    override fun onDestroy() {

        player?.release()
        player = null

        playerView.player = null

        super.onDestroy()
    }
}
