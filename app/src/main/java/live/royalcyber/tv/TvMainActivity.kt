package live.royalcyber.tv

import android.os.Bundle
import android.view.View
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
    private lateinit var channelList: RecyclerView
    private lateinit var loadingText: TextView

    private var player: ExoPlayer? = null

    private var channels: List<Channel> = emptyList()

    private val channelsJsonUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * TV layout
         */
        setContentView(R.layout.activity_tv_main)

        /*
         * Find views
         */
        playerView = findViewById(R.id.tv_player)

        channelList = findViewById(R.id.tv_channel_list)

        loadingText = findViewById(R.id.tv_loading)

        /*
         * Channel list
         */
        channelList.layoutManager =
            LinearLayoutManager(this)

        /*
         * Load channels
         */
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

            } catch (_: Exception) {

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

        /*
         * Give focus to the first channel
         * for Android TV remote.
         */
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
         * Create player only when
         * the first channel is selected.
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

        player?.release()

        player = null

        super.onDestroy()
    }
}
