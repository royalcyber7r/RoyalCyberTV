package live.royalcyber.tv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    private val streamUrl =
        "https://tvsen6.aynaott.com/Epm7WrFa/index.m3u8"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)

        initializePlayer()
    }

    private fun initializePlayer() {

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

        val hlsMediaSource =
            HlsMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(
                    MediaItem.fromUri(streamUrl)
                )

        player = ExoPlayer.Builder(this).build().apply {

            setMediaSource(hlsMediaSource)

            prepare()

            playWhenReady = true

            addListener(object : Player.Listener {

                override fun onPlayerError(error: PlaybackException) {

                    Toast.makeText(
                        this@MainActivity,
                        "ভিডিও চালু করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        playerView.player = player

        playerView.keepScreenOn = true
    }

    override fun onResume() {
        super.onResume()

        player?.play()
    }

    override fun onPause() {
        super.onPause()

        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        player?.release()
        player = null
    }
}
