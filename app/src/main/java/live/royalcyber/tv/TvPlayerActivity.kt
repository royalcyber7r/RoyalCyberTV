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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TV screen awake রাখবে
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_tv_player)

        playerView = findViewById(R.id.tv_player)

        channelName = findViewById(R.id.tv_player_name)

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
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun playChannel(streamUrl: String) {

        player?.release()
        player = null

        val newPlayer =
            ExoPlayer.Builder(this)
                .build()

        player = newPlayer

        playerView.player = newPlayer

        // Player error listener
        newPlayer.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: PlaybackException
                ) {

                    android.util.Log.e(
                        "RoyalCyberTV",
                        "Playback Error: ${error.errorCodeName}",
                        error
                    )

                    runOnUiThread {

                        Toast.makeText(
                            this@TvPlayerActivity,
                            "Channel চালানো যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    when (playbackState) {

                        Player.STATE_BUFFERING -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "Buffering..."
                            )
                        }

                        Player.STATE_READY -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "Playback READY"
                            )
                        }

                        Player.STATE_ENDED -> {

                            android.util.Log.d(
                                "RoyalCyberTV",
                                "Playback ENDED"
                            )
                        }
                    }
                }
            }
        )

        val mediaItem =
            MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMimeType("application/x-mpegURL")
                .build()

        newPlayer.setMediaItem(mediaItem)

        newPlayer.prepare()

        newPlayer.playWhenReady = true
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        // TV remote-এর Back
        if (
            event.keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP
        ) {

            finish()
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {

        super.onStop()

        player?.release()
        player = null
    }

    override fun onDestroy() {

        playerView.player = null

        super.onDestroy()
    }
}
