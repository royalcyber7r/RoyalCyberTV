package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // TV screen awake রাখবে
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
         * Safety check
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
                playChannel(streamUrl)
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
     * বর্তমানে যে channel selected
     * সেটি play করবে
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

        if (streamUrl.isEmpty()) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        playChannel(
            streamUrl
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

        /*
         * শেষ channel-এর পর
         * আবার প্রথম channel
         */
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

        /*
         * প্রথম channel-এর আগে
         * শেষ channel
         */
        if (currentIndex < 0) {

            currentIndex =
                channelUrls.size - 1
        }

        playCurrentChannel()
    }

    private fun playChannel(
        streamUrl: String
    ) {

        /*
         * পুরনো player release
         */
        player?.release()
        player = null

        val newPlayer =
            ExoPlayer.Builder(this)
                .build()

        player =
            newPlayer

        playerView.player =
            newPlayer

        /*
         * Playback error
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
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "Buffering: " +
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
            }
        )

        /*
         * HLS MediaItem
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

        newPlayer.setMediaItem(
            mediaItem
        )

        newPlayer.prepare()

        newPlayer.playWhenReady =
            true
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        /*
         * শুধুমাত্র ACTION_UP-এ
         * channel change হবে।
         *
         * এতে একটি button ধরে রাখলে
         * অযথা অনেকবার channel change হবে না।
         */
        if (
            event.action ==
                KeyEvent.ACTION_UP
        ) {

            when (event.keyCode) {

                /*
                 * Channel Up / CH+
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
                 * Channel Down / CH-
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
                 * TV Remote-এর Back
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

    override fun onStop() {

        super.onStop()

        /*
         * Activity পুরোপুরি
         * background-এ গেলে player release
         */
        player?.release()
        player = null
    }

    override fun onDestroy() {

        playerView.player = null

        super.onDestroy()
    }
}
