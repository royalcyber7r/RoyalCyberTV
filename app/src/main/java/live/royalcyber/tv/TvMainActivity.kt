package live.royalcyber.tv

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
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

    private lateinit var channelList: RecyclerView
    private lateinit var loadingText: TextView
    private lateinit var playerContainer: FrameLayout

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    private var channels: List<Channel> = emptyList()

    private val channelsJsonUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_main)

        channelList =
            findViewById(R.id.tv_channel_list)

        loadingText =
            findViewById(R.id.tv_loading)

        playerContainer =
            findViewById(R.id.tv_player_container)

        channelList.layoutManager =
            LinearLayoutManager(this)

        loadChannels()
    }

    private fun loadChannels() {

        thread {

            try {

                val url =
                    URL(channelsJsonUrl)

                val connection =
                    url.openConnection()
                        as HttpURLConnection

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

                val json =
                    JSONArray(response)

                val list =
                    ArrayList<Channel>()

                for (i in 0 until json.length()) {

                    val item =
                        json.getJSONObject(i)

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

                        list.add(
                            Channel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    channels = list

                    showChannels()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    loadingText.text =
                        "Channel loading failed"
                }
            }
        }
    }

    private fun showChannels() {

        loadingText.visibility =
            View.GONE

        val adapter =
            ChannelAdapter(
                channels
            ) { channel ->

                playChannel(channel)
            }

        channelList.adapter =
            adapter

        channelList.post {

            if (channels.isNotEmpty()) {

                channelList
                    .getChildAt(0)
                    ?.requestFocus()

                channelList.requestFocus()
            }
        }
    }

    private fun playChannel(
        channel: Channel
    ) {

        /*
         * Player is created ONLY after
         * the user selects a channel.
         */

        if (player == null) {

            player =
                ExoPlayer.Builder(this)
                    .build()

            playerView =
                PlayerView(this)

            playerView?.useController = true
            playerView?.setBackgroundColor(Color.BLACK)
            playerView?.player = player

            playerContainer.removeAllViews()

            playerContainer.addView(
                playerView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
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

        player?.release()

        player = null
        playerView = null

        super.onDestroy()
    }
}
