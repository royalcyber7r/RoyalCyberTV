package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
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
        }
    }

    private fun playChannel(streamUrl: String) {

        player = ExoPlayer.Builder(this)
            .build()

        playerView.player = player

        val mediaItem =
            MediaItem.fromUri(
                Uri.parse(streamUrl)
            )

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
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
