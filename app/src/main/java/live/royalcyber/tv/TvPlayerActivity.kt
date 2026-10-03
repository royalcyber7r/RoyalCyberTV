package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

class TvPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RoyalCyberTV"
    }

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var channelName: TextView

    private var channelNames = ArrayList<String>()
    private var channelUrls = ArrayList<String>()
    private var channelLogos = ArrayList<String>()

    private var currentIndex = 0

    private var isDestroyed = false
    private var errorToastShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * TV screen বন্ধ হবে না
         */
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_tv_player)

        playerView = findViewById(R.id.tv_player)
        channelName = findViewById(R.id.tv_player_name)

        channelNames =
            intent.getStringArrayListExtra("channel_names")
                ?: ArrayList()

        channelUrls =
            intent.getStringArrayListExtra("channel_urls")
                ?: ArrayList()

        channelLogos =
            intent.getStringArrayListExtra("channel_logos")
                ?: ArrayList()

        currentIndex =
            intent.getIntExtra("channel_index", 0)

        /*
         * Single channel mode
         */
        if (channelNames.isEmpty() || channelUrls.isEmpty()) {

            val name =
                intent.getStringExtra("channel_name")
                    ?: "RoyalCyber TV"

            val streamUrl =
                intent.getStringExtra("channel_url")
                    ?: ""

            channelName.text = name

            if (streamUrl.isNotBlank()) {
                playChannel(streamUrl)
            } else {
                showMessage("Stream URL পাওয়া যায়নি")
            }

            return
        }

        /*
         * Index নিরাপদ রাখা
         */
        if (
            currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            currentIndex = 0
        }

        playCurrentChannel()
    }

    /**
     * বর্তমান channel play
     */
    private fun playCurrentChannel() {

        if (isDestroyed) return

        if (channelUrls.isEmpty()) return

        if (
            currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            currentIndex = 0
        }

        val name =
            if (currentIndex < channelNames.size) {
                channelNames[currentIndex]
            } else {
                "RoyalCyber TV"
            }

        val streamUrl =
            channelUrls[currentIndex]

        channelName.text = name

        errorToastShown = false

        if (streamUrl.isBlank()) {

            showMessage(
                "এই Channel-এর Stream URL নেই"
            )

            return
        }

        Log.d(
            TAG,
            "--------------------------------"
        )

        Log.d(
            TAG,
            "Channel: $name"
        )

        Log.d(
            TAG,
            "Index: $currentIndex"
        )

        Log.d(
            TAG,
            "URL: $streamUrl"
        )

        playChannel(streamUrl)
    }

    /**
     * Channel Up
     */
    private fun nextChannel() {

        if (isDestroyed) return

        if (channelUrls.isEmpty()) return

        currentIndex++

        if (currentIndex >= channelUrls.size) {
            currentIndex = 0
        }

        playCurrentChannel()
    }

    /**
     * Channel Down
     */
    private fun previousChannel() {

        if (isDestroyed) return

        if (channelUrls.isEmpty()) return

        currentIndex--

        if (currentIndex < 0) {
            currentIndex = channelUrls.size - 1
        }

        playCurrentChannel()
    }

    /**
     * Main TV Player
     *
     * গুরুত্বপূর্ণ:
     *
     * এখানে কোনো custom buffering নেই।
     *
     * ExoPlayer / Media3-এর DEFAULT LoadControl ব্যবহার হচ্ছে।
     *
     * তাই ইচ্ছাকৃতভাবে buffer খুব কমও করা হচ্ছে না,
     * আবার অযথা অনেক বেশি করাও হচ্ছে না।
     */
    private fun playChannel(streamUrl: String) {

        if (isDestroyed) return

        /*
         * পুরোনো player সম্পূর্ণ release
         */
        releasePlayer()

        try {

            /*
             * HTTP Data Source
             *
             * Cross protocol redirect allow করা হচ্ছে।
             *
             * এটি buffering বাড়ানোর setting নয়।
             */
            val httpDataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)

            /*
             * HLS Media Source
             *
             * সব channel URL .m3u8 হওয়ায় সরাসরি
             * HlsMediaSource ব্যবহার করছি।
             */
            val mediaSourceFactory =
                HlsMediaSource.Factory(
                    httpDataSourceFactory
                )

            /*
             * ExoPlayer
             *
             * কোনো custom LoadControl নেই।
             *
             * Media3 Default LoadControl ব্যবহার করবে।
             */
            val newPlayer =
                ExoPlayer.Builder(this)
                    .build()

            player = newPlayer

            playerView.player = newPlayer

            /*
             * Player Listener
             */
            newPlayer.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {

                        when (playbackState) {

                            Player.STATE_IDLE -> {

                                Log.d(
                                    TAG,
                                    "STATE_IDLE: ${channelName.text}"
                                )
                            }

                            Player.STATE_BUFFERING -> {

                                Log.d(
                                    TAG,
                                    "BUFFERING: ${channelName.text}"
                                )
                            }

                            Player.STATE_READY -> {

                                Log.d(
                                    TAG,
                                    "READY: ${channelName.text}"
                                )

                                errorToastShown = false
                            }

                            Player.STATE_ENDED -> {

                                Log.d(
                                    TAG,
                                    "ENDED: ${channelName.text}"
                                )
                            }
                        }
                    }

                    override fun onIsPlayingChanged(
                        isPlaying: Boolean
                    ) {

                        Log.d(
                            TAG,
                            "Playing=$isPlaying | " +
                                "Channel=${channelName.text}"
                        )
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        /*
                         * গুরুত্বপূর্ণ:
                         *
                         * Player error হলে এখানে Activity finish()
                         * করা হচ্ছে না।
                         *
                         * তাই error-এর কারণে app বন্ধ হবে না।
                         */
                        Log.e(
                            TAG,
                            "=============================="
                        )

                        Log.e(
                            TAG,
                            "PLAYBACK ERROR"
                        )

                        Log.e(
                            TAG,
                            "Channel: ${channelName.text}"
                        )

                        Log.e(
                            TAG,
                            "Error Code: ${error.errorCode}"
                        )

                        Log.e(
                            TAG,
                            "Error Name: ${error.errorCodeName}"
                        )

                        Log.e(
                            TAG,
                            "Message: ${error.message}",
                            error
                        )

                        Log.e(
                            TAG,
                            "=============================="
                        )

                        /*
                         * Player-কে সঙ্গে সঙ্গে আবার তৈরি করছি না।
                         *
                         * কারণ বারবার recreate করলে পুরোনো TV-তে
                         * repeated buffering / crash হওয়ার সম্ভাবনা
                         * বাড়তে পারে।
                         */
                        if (!errorToastShown) {

                            errorToastShown = true

                            runOnUiThread {

                                if (!isDestroyed) {

                                    Toast.makeText(
                                        this@TvPlayerActivity,
                                        "এই Channel TV-তে চালানো যাচ্ছে না",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            )

            /*
             * MediaItem
             *
             * MIME type জোর করে সেট করছি না।
             * HlsMediaSource নিজেই HLS source হিসেবে ব্যবহার করবে।
             */
            val mediaItem =
                MediaItem.Builder()
                    .setUri(
                        Uri.parse(streamUrl)
                    )
                    .build()

            /*
             * HLS MediaSource তৈরি
             */
            val mediaSource =
                mediaSourceFactory.createMediaSource(
                    mediaItem
                )

            /*
             * Source set
             */
            newPlayer.setMediaSource(
                mediaSource
            )

            /*
             * Prepare
             */
            newPlayer.prepare()

            /*
             * Auto Play
             */
            newPlayer.playWhenReady = true

        } catch (e: Exception) {

            /*
             * কোনো Java/Kotlin exception হলে
             * Activity বন্ধ হবে না।
             */
            Log.e(
                TAG,
                "Player setup failed",
                e
            )

            showMessage(
                "Channel চালু করা যাচ্ছে না"
            )
        }
    }

    /**
     * Player release
     */
    private fun releasePlayer() {

        try {

            playerView.player = null

            player?.stop()

            player?.release()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Player release error",
                e
            )
        }

        player = null
    }

    /**
     * Safe Toast
     */
    private fun showMessage(
        message: String
    ) {

        if (isDestroyed) return

        try {

            runOnUiThread {

                if (!isDestroyed) {

                    Toast.makeText(
                        this@TvPlayerActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Toast error",
                e
            )
        }
    }

    /**
     * TV Remote Keys
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action == KeyEvent.ACTION_UP) {

            when (event.keyCode) {

                /*
                 * Channel Up
                 */
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_PLUS -> {

                    nextChannel()

                    return true
                }

                /*
                 * Channel Down
                 */
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
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

        return super.dispatchKeyEvent(event)
    }

    /**
     * Activity background
     *
     * TV app-এর lifecycle-এর সময় player release।
     */
    override fun onStop() {

        super.onStop()

        releasePlayer()
    }

    /**
     * Activity destroy
     */
    override fun onDestroy() {

        isDestroyed = true

        releasePlayer()

        super.onDestroy()
    }
}
