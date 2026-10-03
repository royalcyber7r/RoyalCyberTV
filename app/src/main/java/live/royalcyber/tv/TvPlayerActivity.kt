package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
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
     * একই channel-এর জন্য fallback
     * একবারের বেশি করা হবে না।
     */
    private var fallbackTried = false

    /*
     * Channel পরিবর্তন হলে পুরনো
     * retry যেন নতুন channel-এ
     * প্রভাব না ফেলে।
     */
    private var playbackGeneration = 0

    private val handler =
        Handler(Looper.getMainLooper())


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * TV screen awake রাখবে
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

            channelName.text =
                name

            if (streamUrl.isNotBlank()) {

                fallbackTried = false
                playbackGeneration++

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
         * নতুন channel।
         */
        fallbackTried = false
        playbackGeneration++

        /*
         * পুরনো retry cancel
         */
        handler.removeCallbacksAndMessages(
            null
        )

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
     * Main TV Player
     *
     * Media3 1.5.1 compatible
     */
    private fun playChannel(
        streamUrl: String,
        fallbackMode: Boolean
    ) {

        /*
         * পুরনো retry cancel
         */
        handler.removeCallbacksAndMessages(
            null
        )

        /*
         * পুরনো player release
         */
        player?.release()
        player = null

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
         * TV-এর জন্য stable buffer
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
         * ExoPlayer
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
         * TV video scaling
         */
        newPlayer.setVideoScalingMode(
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        )

        /*
         * Player Listener
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
                            (error.message
                                ?: "Unknown")
                    )

                    /*
                     * প্রথম error হলে
                     * MIME type ছাড়া fallback
                     * চেষ্টা করবে।
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
                     * Fallback ব্যর্থ হলে
                     * 4 সেকেন্ড পরে retry।
                     */
                    if (
                        myGeneration ==
                            playbackGeneration
                    ) {

                        handler.postDelayed({

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

                        }, 4000)
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
         * প্রথমবার HLS MIME type
         */
        if (!fallbackMode) {

            mediaItemBuilder.setMimeType(
                "application/x-mpegURL"
            )
        }

        /*
         * Live configuration
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
         * Stream set
         */
        newPlayer.setMediaItem(
            mediaItem
        )

        /*
         * Prepare
         */
        newPlayer.prepare()

        /*
         * Auto play
         */
        newPlayer.playWhenReady =
            true
    }


    /**
     * Stream retry
     */
    private fun retryCurrentStream(
        streamUrl: String
    ) {

        if (isFinishing) {
            return
        }

        /*
         * retry-তে fallback mode ব্যবহার
         * করা হচ্ছে যাতে infinite fallback
         * loop না হয়।
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
         * channel change
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
     * Activity stop
     */
    override fun onStop() {

        /*
         * Pending retry cancel
         */
        handler.removeCallbacksAndMessages(
            null
        )

        player?.release()
        player = null

        super.onStop()
    }


    /**
     * Activity destroy
     */
    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        player?.release()
        player = null

        playerView.player = null

        super.onDestroy()
    }
}
