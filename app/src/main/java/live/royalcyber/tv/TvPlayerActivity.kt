package live.royalcyber.tv

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView


class TvPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private lateinit var rootLayout: FrameLayout
    private lateinit var channelNameText: TextView

    private var channelName: String = ""
    private var channelLogo: String = ""
    private var streamUrl: String = ""


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * TvMainActivity থেকে Channel information নেওয়া হচ্ছে।
         */
        channelName =
            intent.getStringExtra(
                TvMainActivity.EXTRA_CHANNEL_NAME
            )
                ?.trim()
                ?: ""

        channelLogo =
            intent.getStringExtra(
                TvMainActivity.EXTRA_CHANNEL_LOGO
            )
                ?.trim()
                ?: ""

        streamUrl =
            intent.getStringExtra(
                TvMainActivity.EXTRA_CHANNEL_URL
            )
                ?.trim()
                ?: ""


        /*
         * TV অবশ্যই Landscape থাকবে।
         */
        requestedOrientation =
            android.content.pm.ActivityInfo
                .SCREEN_ORIENTATION_LANDSCAPE


        /*
         * সম্পূর্ণ Player UI programmatically তৈরি করা হচ্ছে।
         *
         * তাই activity_tv_player.xml দরকার নেই।
         */
        createPlayerLayout()


        /*
         * Stream URL না থাকলে Player চালানোর চেষ্টা করবে না।
         */
        if (streamUrl.isEmpty()) {

            showError(
                "Stream URL পাওয়া যায়নি"
            )

            return
        }


        /*
         * Channel select করার পরেই Player তৈরি হবে।
         */
        initializePlayer()
    }


    /* =========================================================
       CREATE PLAYER LAYOUT
       ========================================================= */

    private fun createPlayerLayout() {

        rootLayout =
            FrameLayout(this)

        rootLayout.setBackgroundColor(
            Color.BLACK
        )


        /*
         * PlayerView
         */
        playerView =
            PlayerView(this)

        playerView?.apply {

            useController = true

            setShowBuffering(
                PlayerView.SHOW_BUFFERING_WHEN_PLAYING
            )

            setBackgroundColor(
                Color.BLACK
            )
        }


        rootLayout.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )


        /*
         * Channel Name
         *
         * শুধু Player-এর উপরে ছোট করে নাম দেখাবে।
         */
        channelNameText =
            TextView(this)

        channelNameText.apply {

            text =
                if (channelName.isNotEmpty()) {
                    channelName
                } else {
                    "RoyalCyber TV"
                }

            setTextColor(
                Color.WHITE
            )

            setTextSize(
                18f
            )

            setPadding(
                24,
                16,
                24,
                16
            )

            setBackgroundColor(
                0x88000000.toInt()
            )

            gravity =
                Gravity.CENTER_VERTICAL
        }


        val nameParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )


        nameParams.gravity =
            Gravity.TOP or Gravity.START


        rootLayout.addView(
            channelNameText,
            nameParams
        )


        setContentView(
            rootLayout
        )
    }


    /* =========================================================
       INITIALIZE PLAYER
       ========================================================= */

    private fun initializePlayer() {

        try {

            player =
                ExoPlayer.Builder(this)
                    .build()


            playerView?.player =
                player


            player?.repeatMode =
                Player.REPEAT_MODE_OFF


            /*
             * Player state listener
             */
            player?.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {

                        when (playbackState) {

                            Player.STATE_BUFFERING -> {

                                /*
                                 * Media3 নিজেই buffering UI দেখাবে।
                                 */
                            }


                            Player.STATE_READY -> {
                                // Stream ready
                            }


                            Player.STATE_ENDED -> {
                                // Stream ended
                            }


                            Player.STATE_IDLE -> {
                                // Idle
                            }
                        }
                    }


                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        if (
                            isFinishing ||
                            isDestroyed
                        ) {
                            return
                        }


                        Toast.makeText(
                            this@TvPlayerActivity,
                            "এই Channel চালু করা যাচ্ছে না",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )


            /*
             * HTTP Data Source
             */
            val dataSourceFactory =
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(
                        true
                    )


            val mediaItem =
                MediaItem.fromUri(
                    streamUrl
                )


            /*
             * M3U8 হলে HLS player
             *
             * অন্য stream হলে ProgressiveMediaSource
             */
            val mediaSource =
                if (
                    streamUrl.contains(
                        ".m3u8",
                        ignoreCase = true
                    )
                ) {

                    HlsMediaSource.Factory(
                        dataSourceFactory
                    ).createMediaSource(
                        mediaItem
                    )

                } else {

                    ProgressiveMediaSource.Factory(
                        dataSourceFactory
                    ).createMediaSource(
                        mediaItem
                    )
                }


            player?.apply {

                setMediaSource(
                    mediaSource
                )

                prepare()

                playWhenReady =
                    true

                play()
            }


        } catch (
            e: Exception
        ) {

            showError(
                "Player চালু করা যাচ্ছে না"
            )
        }
    }


    /* =========================================================
       ERROR
       ========================================================= */

    private fun showError(
        message: String
    ) {

        if (
            !::rootLayout.isInitialized
        ) {
            return
        }


        val errorText =
            TextView(this)


        errorText.apply {

            text =
                message

            setTextColor(
                Color.WHITE
            )

            setTextSize(
                22f
            )

            gravity =
                Gravity.CENTER

            setBackgroundColor(
                Color.BLACK
            )
        }


        rootLayout.removeAllViews()


        rootLayout.addView(
            errorText,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }


    /* =========================================================
       RESUME
       ========================================================= */

    override fun onResume() {

        super.onResume()

        player?.let {

            try {

                if (
                    it.playbackState !=
                    Player.STATE_IDLE
                ) {

                    it.play()
                }

            } catch (
                _: Exception
            ) {
            }
        }
    }


    /* =========================================================
       PAUSE
       ========================================================= */

    override fun onPause() {

        try {

            player?.pause()

        } catch (
            _: Exception
        ) {
        }


        super.onPause()
    }


    /* =========================================================
       BACK
       ========================================================= */

    override fun onBackPressed() {

        /*
         * Remote-এর Back চাপলে Player বন্ধ হয়ে
         * TvMainActivity-এর Channel List-এ ফিরে যাবে।
         */
        finish()
    }


    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        try {

            playerView?.player =
                null

        } catch (
            _: Exception
        ) {
        }


        try {

            player?.release()

        } catch (
            _: Exception
        ) {
        }


        player = null
        playerView = null


        super.onDestroy()
    }
}
