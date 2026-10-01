package live.royalcyber.tv

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView

class TvPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var channelTitle: TextView

    private var player: ExoPlayer? = null

    private var streamUrl: String = ""
    private var channelName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_player)

        playerView = findViewById(R.id.tv_player_view)
        channelTitle = findViewById(R.id.tv_player_title)

        channelName =
            intent.getStringExtra("channel_name") ?: "RoyalCyber TV"

        streamUrl =
            intent.getStringExtra("stream_url") ?: ""

        channelTitle.text = channelName

        if (streamUrl.isBlank()) {
            channelTitle.text = "$channelName\nStream unavailable"
            return
        }

        initializePlayer()
    }

    private fun initializePlayer() {

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)

        val mediaSource: MediaSource

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(streamUrl))
            .apply {

                if (
                    streamUrl.contains(".m3u8", ignoreCase = true)
                ) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }

            }
            .build()

        if (streamUrl.contains(".m3u8", ignoreCase = true)) {

            mediaSource =
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)

        } else {

            mediaSource =
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    httpDataSourceFactory
                ).createMediaSource(mediaItem)
        }

        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->

                playerView.player = exoPlayer

                exoPlayer.setMediaSource(mediaSource)

                exoPlayer.prepare()

                exoPlayer.playWhenReady = true

                exoPlayer.addListener(
                    object : Player.Listener {

                        override fun onPlaybackStateChanged(
                            playbackState: Int
                        ) {

                            when (playbackState) {

                                Player.STATE_BUFFERING -> {
                                    showPlayerTitle(false)
                                }

                                Player.STATE_READY -> {
                                    showPlayerTitle(false)
                                }

                                Player.STATE_ENDED -> {
                                    exoPlayer.seekTo(0)
                                    exoPlayer.play()
                                }

                                Player.STATE_IDLE -> {
                                    showPlayerTitle(true)
                                }
                            }
                        }

                        override fun onPlayerError(
                            error: androidx.media3.common.PlaybackException
                        ) {

                            showPlayerTitle(true)
                        }
                    }
                )
            }
    }

    private fun showPlayerTitle(show: Boolean) {

        channelTitle.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    override fun onPause() {

        super.onPause()

        player?.pause()
    }

    override fun onResume() {

        super.onResume()

        player?.play()
    }

    override fun onStop() {

        super.onStop()

        releasePlayer()
    }

    override fun onDestroy() {

        releasePlayer()

        super.onDestroy()
    }

    private fun releasePlayer() {

        player?.release()
        player = null

        playerView.player = null
    }
}
