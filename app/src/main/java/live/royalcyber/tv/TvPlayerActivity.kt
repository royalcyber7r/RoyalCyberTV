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
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TV screen সবসময় জাগ্রত রাখবে
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        // Single channel mode
        if (channelNames.isEmpty() || channelUrls.isEmpty()) {

            val name =
                intent.getStringExtra("channel_name")
                    ?: "RoyalCyber TV"

            val streamUrl =
                intent.getStringExtra("channel_url")
                    ?: ""

            channelName.text = name

            if (streamUrl.isNotEmpty()) {
                playChannel(streamUrl)
            } else {
                Toast.makeText(
                    this,
                    "Stream URL পাওয়া যায়নি",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }

        // Index নিরাপদ রাখা
        if (currentIndex < 0 ||
            currentIndex >= channelUrls.size
        ) {
            currentIndex = 0
        }

        playCurrentChannel()
    }

    /**
     * বর্তমান Channel চালু করবে
     */
    private fun playCurrentChannel() {

        if (channelUrls.isEmpty()) return

        if (currentIndex < 0 ||
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

        if (streamUrl.isBlank()) {

            Toast.makeText(
                this,
                "Stream URL পাওয়া যায়নি",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Log.d(
            TAG,
            "Playing: $name"
        )

        Log.d(
            TAG,
            "URL: $streamUrl"
        )

        playChannel(streamUrl)
    }

    /**
     * Next Channel
     */
    private fun nextChannel() {

        if (channelUrls.isEmpty()) return

        currentIndex++

        if (currentIndex >= channelUrls.size) {
            currentIndex = 0
        }

        playCurrentChannel()
    }

    /**
     * Previous Channel
     */
    private fun previousChannel() {

        if (channelUrls.isEmpty()) return

        currentIndex--

        if (currentIndex < 0) {
            currentIndex = channelUrls.size - 1
        }

        playCurrentChannel()
    }

    /**
     * Strong / Compatible TV Player
     *
     * গুরুত্বপূর্ণ:
     * এখানে কোনো custom buffering নেই।
     * Media3 নিজের default LoadControl ব্যবহার করবে।
     */
    private fun playChannel(streamUrl: String) {

        // আগের player পুরোপুরি বন্ধ
        player?.release()
        player = null

        try {

            /*
             * Android TV-এর জন্য সাধারণ HTTP data source।
             *
             * Default User-Agent রাখছি।
             * কিছু পুরোনো IPTV server Android TV থেকে request
             * ঠিকমতো নিতে পারে না, তাই এখানে একটি সাধারণ
             * Android/Media3-compatible User-Agent ব্যবহার করা হচ্ছে।
             */
            val httpDataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)

            /*
             * DefaultMediaSourceFactory ব্যবহার করা হচ্ছে।
             *
             * এতে Media3 নিজে stream type বুঝতে পারে।
             */
            val mediaSourceFactory =
                DefaultMediaSourceFactory(
                    httpDataSourceFactory
                )

            /*
             * ExoPlayer-এর DEFAULT LoadControl।
             *
             * কোনো zero buffer,
             * custom buffer,
             * aggressive buffering
             * ব্যবহার করা হচ্ছে না।
             */
            val newPlayer =
                ExoPlayer.Builder(this)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()

            player = newPlayer

            playerView.player = newPlayer

            /*
             * Player listener
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
                            "isPlaying=$isPlaying, channel=${channelName.text}"
                        )
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        Log.e(
                            TAG,
                            "Playback Error"
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

                        /*
                         * TV-তে stream না চললে user-কে
                         * পরিষ্কারভাবে জানানো।
                         */
                        runOnUiThread {

                            Toast.makeText(
                                this@TvPlayerActivity,
                                "এই Channel TV-তে চালানো যাচ্ছে না",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

            /*
             * URL থেকে MediaItem।
             *
             * এখানে জোর করে MIME type দেওয়া হচ্ছে না।
             * Media3 URL দেখে HLS detect করবে।
             *
             * এটি বিভিন্ন server-এর m3u8 stream-এর
             * compatibility বাড়াতে সাহায্য করতে পারে।
             */
            val mediaItem =
                MediaItem.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .build()

            /*
             * MediaItem set
             */
            newPlayer.setMediaItem(mediaItem)

            /*
             * Prepare
             */
            newPlayer.prepare()

            /*
             * Automatically play
             */
            newPlayer.playWhenReady = true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Player creation failed",
                e
            )

            Toast.makeText(
                this,
                "Player চালু করা যায়নি",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Remote / TV key control
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action == KeyEvent.ACTION_UP) {

            when (event.keyCode) {

                // Channel Up
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_PLUS -> {

                    nextChannel()

                    return true
                }

                // Channel Down
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MINUS -> {

                    previousChannel()

                    return true
                }

                // Back
                KeyEvent.KEYCODE_BACK -> {

                    finish()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    /**
     * Activity বন্ধ/পিছনে গেলে player release
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

        playerView.player = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
