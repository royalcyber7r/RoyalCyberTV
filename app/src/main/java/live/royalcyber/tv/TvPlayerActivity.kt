package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.view.View
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

        setContentView(R.layout.activity_tv_player)

        playerView = findViewById(R.id.tv_player)
        channelName = findViewById(R.id.tv_player_name)

        val name =
            intent.getStringExtra("channel_name")
                ?: "RoyalCyber TV"

        val url =
            intent.getStringExtra("channel_url")
                ?: ""

        channelName.text = name

        if (url.isNotEmpty()) {
            playChannel(url)
        }
    }

    private fun playChannel(url: String) {

        player = ExoPlayer.Builder(this)
            .build()

        playerView.player = player

        val mediaItem =
            MediaItem.fromUri(Uri.parse(url))

        player?.setMediaItem(mediaItem)

        player?.prepare()

        player?.playWhenReady = true
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
