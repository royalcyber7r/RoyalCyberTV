package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TvMainActivity : AppCompatActivity() {

    private lateinit var channelList: RecyclerView
    private lateinit var loadingText: TextView

    private val channelsJsonUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    private var channels: List<Channel> = emptyList()

    companion object {
        const val EXTRA_CHANNEL_NAME = "tv_channel_name"
        const val EXTRA_CHANNEL_LOGO = "tv_channel_logo"
        const val EXTRA_CHANNEL_URL = "tv_channel_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TV screen only
        setContentView(R.layout.activity_tv_main)

        channelList = findViewById(R.id.tv_channel_list)
        loadingText = findViewById(R.id.tv_loading)

        channelList.layoutManager = LinearLayoutManager(this)

        // TV remote focus
        channelList.isFocusable = true
        channelList.isFocusableInTouchMode = true

        loadChannels()
    }

    private fun loadChannels() {

        loadingText.text = "Loading Channels..."
        loadingText.visibility = View.VISIBLE

        thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(channelsJsonUrl)

                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.useCaches = false
                connection.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
                )

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception("HTTP $responseCode")
                }

                val response =
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }

                val jsonArray = JSONArray(response)

                val result = ArrayList<Channel>()

                for (i in 0 until jsonArray.length()) {

                    val item = jsonArray.optJSONObject(i)
                        ?: continue

                    val name =
                        item.optString("name").trim()

                    val logo =
                        item.optString("logo").trim()

                    val streamUrl =
                        item.optString("streamUrl").trim()

                    if (
                        name.isNotEmpty() &&
                        streamUrl.isNotEmpty()
                    ) {

                        result.add(
                            Channel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    channels = result

                    if (channels.isEmpty()) {

                        loadingText.text =
                            "No channels found"

                        loadingText.visibility =
                            View.VISIBLE

                        return@runOnUiThread
                    }

                    loadingText.visibility = View.GONE

                    channelList.adapter =
                        ChannelAdapter(
                            channels = channels,
                            onChannelClick = {
                                openPlayer(it)
                            }
                        )

                    // Give focus to first channel
                    channelList.post {

                        if (
                            !isFinishing &&
                            !isDestroyed &&
                            channels.isNotEmpty()
                        ) {

                            channelList.scrollToPosition(0)

                            channelList.requestFocus()

                            channelList.layoutManager
                                ?.findViewByPosition(0)
                                ?.requestFocus()
                        }
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    loadingText.text =
                        "Channel loading failed"

                    loadingText.visibility =
                        View.VISIBLE
                }

            } finally {

                try {
                    connection?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun openPlayer(channel: Channel) {

        val streamUrl =
            channel.streamUrl.trim()

        if (streamUrl.isEmpty()) {
            return
        }

        val intent =
            Intent(
                this,
                TvPlayerActivity::class.java
            )

        intent.putExtra(
            EXTRA_CHANNEL_NAME,
            channel.name
        )

        intent.putExtra(
            EXTRA_CHANNEL_LOGO,
            channel.logo
        )

        intent.putExtra(
            EXTRA_CHANNEL_URL,
            streamUrl
        )

        startActivity(intent)
    }

    override fun onDestroy() {

        if (::channelList.isInitialized) {
            channelList.adapter = null
        }

        super.onDestroy()
    }
}
