package live.royalcyber.tv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

@UnstableApi
class TvPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val streamUrl =
            intent.getStringExtra(
                TvMainActivity.EXTRA_CHANNEL_URL
            )?.trim().orEmpty()

        if (streamUrl.isEmpty()) {
            showError("Stream URL পাওয়া যায়নি")
            return
        }

        val playerView =
            PlayerView(this)

        playerView.useController = true

        setContentView(playerView)

        val httpFactory =
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true)

        val exoPlayer =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                        httpFactory
                    )
                )
                .build()

        player = exoPlayer

        playerView.player =
            exoPlayer

        val mediaItem =
            MediaItem.fromUri(streamUrl)

        if (
            streamUrl.contains(
                ".m3u8",
                ignoreCase = true
            )
        ) {

            val mediaSource =
                HlsMediaSource.Factory(
                    httpFactory
                ).createMediaSource(
                    mediaItem
                )

            exoPlayer.setMediaSource(
                mediaSource
            )

        } else {

            val mediaSource =
                ProgressiveMediaSource.Factory(
                    httpFactory
                ).createMediaSource(
                    mediaItem
                )

            exoPlayer.setMediaSource(
                mediaSource
            )
        }

        exoPlayer.prepare()

        exoPlayer.playWhenReady = true
    }

    private fun showError(
        message: String
    ) {

        val textView =
            TextView(this)

        textView.text = message
        textView.textSize = 22f
        textView.setTextColor(
            android.graphics.Color.WHITE
        )
        textView.gravity = Gravity.CENTER

        val root =
            FrameLayout(this)

        root.setBackgroundColor(
            android.graphics.Color.BLACK
        )

        root.addView(
            textView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)
    }

    override fun onDestroy() {

        player?.release()
        player = null

        super.onDestroy()
    }
}
