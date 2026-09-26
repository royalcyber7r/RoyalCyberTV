package live.royalcyber.tv

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TvMainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var channelRecyclerView: RecyclerView
    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var loadingText: TextView

    private var player: ExoPlayer? = null

    private var channels: List<Channel> = emptyList()

    private val channelsJsonUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Simple TV screen
         */
        createTvInterface()

        /*
         * Player is created only when needed.
         * This keeps TV startup light.
         */

        loadChannels()
    }

    private fun createTvInterface() {

        /*
         * Main horizontal layout
         *
         * LEFT  = Channel List
         * RIGHT = Video Player
         */
        val mainLayout = LinearLayout(this)

        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setBackgroundColor(Color.BLACK)

        /*
         * Channel list area
         */
        val channelArea = LinearLayout(this)

        channelArea.orientation = LinearLayout.VERTICAL
        channelArea.setBackgroundColor(Color.rgb(18, 18, 18))
        channelArea.setPadding(18, 18, 18, 18)

        /*
         * Header
         */
        val title = TextView(this)

        title.text = "ROYALCYBER TV"
        title.textSize = 22f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(8, 12, 8, 20)

        channelArea.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        /*
         * Loading text
         */
        loadingText = TextView(this)

        loadingText.text = "Loading Channels..."
        loadingText.textSize = 18f
        loadingText.setTextColor(Color.LTGRAY)
        loadingText.gravity = Gravity.CENTER
        loadingText.visibility = View.VISIBLE

        channelArea.addView(
            loadingText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        /*
         * RecyclerView
         */
        channelRecyclerView = RecyclerView(this)

        channelRecyclerView.layoutManager =
            LinearLayoutManager(this)

        channelRecyclerView.isFocusable = true
        channelRecyclerView.isFocusableInTouchMode = true

        channelArea.addView(
            channelRecyclerView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        /*
         * Video Player
         */
        playerView = PlayerView(this)

        playerView.useController = true
        playerView.setBackgroundColor(Color.BLACK)

        /*
         * Add channel area
         *
         * 32% screen width
         */
        mainLayout.addView(
            channelArea,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.32f
            )
        )

        /*
         * Add player
         *
         * 68% screen width
         */
        mainLayout.addView(
            playerView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.68f
            )
        )

        setContentView(mainLayout)
    }

    private fun loadChannels() {

        thread {

            try {

                val url = URL(channelsJsonUrl)

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                connection.connect()

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val jsonArray =
                    JSONArray(response)

                val loadedChannels =
                    mutableListOf<Channel>()

                for (i in 0 until jsonArray.length()) {

                    val item =
                        jsonArray.getJSONObject(i)

                    val name =
                        item.optString("name")

                    val logo =
                        item.optString("logo")

                    val streamUrl =
                        item.optString("streamUrl")

                    if (
                        name.isNotEmpty() &&
                        streamUrl.isNotEmpty()
                    ) {

                        loadedChannels.add(
                            Channel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    channels =
                        loadedChannels

                    showChannels()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    loadingText.text =
                        "Channel loading failed"

                    loadingText.setTextColor(
                        Color.RED
                    )
                }
            }
        }
    }

    private fun showChannels() {

        loadingText.visibility = View.GONE

        channelAdapter =
            ChannelAdapter(
                channels
            ) { channel ->

                playChannel(channel)
            }

        channelRecyclerView.adapter =
            channelAdapter

        /*
         * Give focus to first channel
         */
        channelRecyclerView.post {

            if (channels.isNotEmpty()) {

                channelRecyclerView
                    .getChildAt(0)
                    ?.requestFocus()

                if (
                    channelRecyclerView
                        .getChildAt(0) == null
                ) {
                    channelRecyclerView.requestFocus()
                }
            }
        }
    }

    private fun playChannel(channel: Channel) {

        /*
         * Create player only when
         * user selects a channel.
         */
        if (player == null) {

            player =
                ExoPlayer.Builder(this)
                    .build()

            playerView.player =
                player
        }

        try {

            val mediaItem =
                MediaItem.fromUri(
                    channel.streamUrl
                )

            player?.apply {

                setMediaItem(mediaItem)

                prepare()

                playWhenReady = true
            }

        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        try {
            channelAdapter.shutdown()
        } catch (_: Exception) {
        }

        player?.release()

        player = null
    }
}
