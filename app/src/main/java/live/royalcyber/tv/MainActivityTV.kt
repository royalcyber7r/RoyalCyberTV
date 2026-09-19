package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivityTV : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var channelList: LinearLayout
    private lateinit var scrollView: ScrollView

    private var player: ExoPlayer? = null

    private val channels = ArrayList<TVChannel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_main)

        playerView = findViewById(R.id.tv_player)
        channelList = findViewById(R.id.tv_channel_list)
        scrollView = findViewById(R.id.tv_scroll)

        setupPlayer()

        findViewById<Button>(
            R.id.tv_update_button
        ).setOnClickListener {

            try {
                startActivity(
                    Intent(
                        this,
                        UpdateActivity::class.java
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Update চালু করা যাচ্ছে না",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        loadChannels()
    }

    private fun setupPlayer() {

        player = ExoPlayer.Builder(this).build()

        playerView.player = player

        playerView.keepScreenOn = true

        playerView.requestFocus()

        player?.addListener(
            object : Player.Listener {

                override fun onPlayerError(
                    error: androidx.media3.common.PlaybackException
                ) {
                    Toast.makeText(
                        this@MainActivityTV,
                        "এই Channel চালু করা যাচ্ছে না",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun loadChannels() {

        Thread {

            try {

                val connection =
                    URL(
                        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"
                    ).openConnection()
                            as HttpURLConnection

                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val json =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val array = JSONArray(json)

                val result =
                    ArrayList<TVChannel>()

                for (i in 0 until array.length()) {

                    val item =
                        array.optJSONObject(i)
                            ?: continue

                    val name =
                        item.optString("name")
                            .trim()

                    val logo =
                        item.optString("logo")
                            .trim()

                    val streamUrl =
                        item.optString("streamUrl")
                            .trim()

                    if (
                        name.isNotEmpty() &&
                        streamUrl.isNotEmpty()
                    ) {

                        result.add(
                            TVChannel(
                                name,
                                logo,
                                streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    channels.clear()
                    channels.addAll(result)

                    showChannels()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "channels.json লোড করা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showChannels() {

        channelList.removeAllViews()

        channels.forEachIndexed { index, channel ->

            val button =
                TextView(this)

            button.text =
                "${index + 1}.  ${channel.name}"

            button.textSize = 20f
            button.setTextColor(
                android.graphics.Color.WHITE
            )

            button.setPadding(
                25,
                20,
                25,
                20
            )

            button.isFocusable = true
            button.isFocusableInTouchMode = true

            button.setBackgroundResource(
                android.R.drawable.btn_default
            )

            button.setOnClickListener {

                playChannel(channel)
            }

            channelList.addView(button)
        }

        if (channelList.childCount > 0) {

            channelList
                .getChildAt(0)
                .requestFocus()
        }
    }

    private fun playChannel(
        channel: TVChannel
    ) {

        val url = channel.streamUrl.trim()

        if (url.isEmpty()) {
            return
        }

        try {

            val mediaItem =
                MediaItem.fromUri(url)

            player?.apply {

                stop()

                clearMediaItems()

                setMediaItem(mediaItem)

                prepare()

                playWhenReady = true
            }

            playerView.requestFocus()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Player Error",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        return playerView.dispatchKeyEvent(event) ||
                super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {

        playerView.player = null

        player?.release()

        player = null

        super.onDestroy()
    }
}

data class TVChannel(
    val name: String,
    val logo: String,
    val streamUrl: String
)
