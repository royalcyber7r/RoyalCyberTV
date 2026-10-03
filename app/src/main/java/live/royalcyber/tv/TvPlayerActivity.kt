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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class TvPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RoyalCyberTV"
    }

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

    private var activityDestroyed = false

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
            )
                ?: ArrayList()

        channelUrls =
            intent.getStringArrayListExtra(
                "channel_urls"
            )
                ?: ArrayList()

        channelLogos =
            intent.getStringArrayListExtra(
                "channel_logos"
            )
                ?: ArrayList()

        currentIndex =
            intent.getIntExtra(
                "channel_index",
                0
            )

        /*
         * Single Channel mode
         */
        if (
            channelNames.isEmpty() ||
            channelUrls.isEmpty()
        ) {

            val name =
                intent.getStringExtra(
                    "channel_name"
                )
                    ?: "RoyalCyber TV"

            val streamUrl =
                intent.getStringExtra(
                    "channel_url"
                )
                    ?: ""

            channelName.text =
                name

            if (streamUrl.isNotEmpty()) {

                playChannel(
                    streamUrl
                )

            } else {

                showToast(
                    "Stream URL পাওয়া যায়নি"
                )
            }

            return
        }

        /*
         * Index safety
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
     * বর্তমানে selected channel play করবে
     */
    private fun playCurrentChannel() {

        if (activityDestroyed) {
            return
        }

        if (channelUrls.isEmpty()) {
            return
        }

        if (
            currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            currentIndex = 0
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

        if (streamUrl.isEmpty()) {

            showToast(
                "Stream URL পাওয়া যায়নি"
            )

            return
        }

        Log.d(
            TAG,
            "Playing Channel: $name"
        )

        playChannel(
            streamUrl
        )
    }

    /**
     * Next Channel
     */
    private fun nextChannel() {

        if (activityDestroyed) {
            return
        }

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

        if (activityDestroyed) {
            return
        }

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
     * Main Player
     *
     * এখানে কোনো custom buffering নেই।
     *
     * Media3-এর DEFAULT LoadControl ব্যবহার হচ্ছে।
     *
     * আগের stable player structure
     * একই রাখা হয়েছে।
     */
    private fun playChannel(
        streamUrl: String
    ) {

        if (activityDestroyed) {
            return
        }

        try {

            /*
             * পুরনো player release
             */
            try {

                player?.release()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Old player release error",
                    e
                )
            }

            player = null

            /*
             * নতুন ExoPlayer
             *
             * কোনো custom LoadControl নেই।
             */
            val newPlayer =
                ExoPlayer.Builder(this)
                    .build()

            player =
                newPlayer

            playerView.player =
                newPlayer

            /*
             * Playback Listener
             */
            newPlayer.addListener(
                object : Player.Listener {

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        /*
                         * Error হলে Activity বন্ধ করা হবে না।
                         *
                         * শুধু Logcat-এ error থাকবে।
                         */
                        Log.e(
                            TAG,
                            "Playback Error: " +
                                error.errorCodeName,
                            error
                        )

                        Log.e(
                            TAG,
                            "Error Code: " +
                                error.errorCode
                        )

                        Log.e(
                            TAG,
                            "Channel: " +
                                channelName.text
                        )
                    }

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {

                        when (
                            playbackState
                        ) {

                            Player.STATE_BUFFERING -> {

                                Log.d(
                                    TAG,
                                    "Buffering: " +
                                        channelName.text
                                )
                            }

                            Player.STATE_READY -> {

                                Log.d(
                                    TAG,
                                    "READY: " +
                                        channelName.text
                                )
                            }

                            Player.STATE_ENDED -> {

                                Log.d(
                                    TAG,
                                    "ENDED: " +
                                        channelName.text
                                )
                            }
                        }
                    }
                }
            )

            /*
             * HLS MediaItem
             *
             * আগের stable code-এর মতো
             * application/x-mpegURL রাখা হয়েছে।
             */
            val mediaItem =
                MediaItem.Builder()
                    .setUri(
                        Uri.parse(streamUrl)
                    )
                    .setMimeType(
                        "application/x-mpegURL"
                    )
                    .build()

            /*
             * MediaItem set
             */
            newPlayer.setMediaItem(
                mediaItem
            )

            /*
             * Prepare
             */
            newPlayer.prepare()

            /*
             * Auto Play
             */
            newPlayer.playWhenReady =
                true

        } catch (e: Exception) {

            /*
             * Player তৈরি/prepare করার সময়
             * কোনো Java/Kotlin exception হলে
             * Activity বন্ধ হবে না।
             */
            Log.e(
                TAG,
                "Player Exception",
                e
            )

            player = null

            if (!activityDestroyed) {

                showToast(
                    "এই Channel চালানো যাচ্ছে না"
                )
            }
        }
    }

    /**
     * Safe Toast
     */
    private fun showToast(
        message: String
    ) {

        if (activityDestroyed) {
            return
        }

        try {

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Toast error",
                e
            )
        }
    }

    /**
     * TV Remote Key Control
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        /*
         * শুধু ACTION_UP-এ কাজ করবে।
         *
         * তাই button ধরে রাখলে
         * বারবার channel change হবে না।
         */
        if (
            event.action ==
                KeyEvent.ACTION_UP
        ) {

            when (event.keyCode) {

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
     * Activity background
     */
    override fun onStop() {

        super.onStop()

        /*
         * Background-এ গেলে player release
         */
        try {

            player?.release()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "onStop release error",
                e
            )
        }

        player = null
    }

    /**
     * Activity destroy
     */
    override fun onDestroy() {

        activityDestroyed = true

        try {

            playerView.player = null

        } catch (e: Exception) {

            Log.e(
                TAG,
                "PlayerView cleanup error",
                e
            )
        }

        try {

            player?.release()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Player release error",
                e
            )
        }

        player = null

        super.onDestroy()
    }
}
