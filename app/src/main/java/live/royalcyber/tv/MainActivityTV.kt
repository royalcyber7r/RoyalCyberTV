package live.royalcyber.tv

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivityTV : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelAdapter

    private val channels = mutableListOf<Channel>()

    private val channelsUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_main)

        recyclerView = findViewById(R.id.tv_channel_recycler)

        recyclerView.layoutManager =
            GridLayoutManager(this, 5)

        adapter = ChannelAdapter(
            channels = channels,
            onChannelClick = { channel ->

                Toast.makeText(
                    this,
                    channel.name,
                    Toast.LENGTH_SHORT
                ).show()

            }
        )

        recyclerView.adapter = adapter

        recyclerView.isFocusable = true
        recyclerView.isFocusableInTouchMode = true

        loadChannels()
    }

    private fun loadChannels() {

        Thread {

            try {

                val connection =
                    URL(channelsUrl)
                        .openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val jsonText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val jsonArray =
                    JSONArray(jsonText)

                val result =
                    mutableListOf<Channel>()

                for (i in 0 until jsonArray.length()) {

                    val item =
                        jsonArray.optJSONObject(i)
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

                    channels.clear()
                    channels.addAll(result)

                    adapter.updateList(channels)

                    recyclerView.post {

                        recyclerView.requestFocus()

                        recyclerView.layoutManager
                            ?.findViewByPosition(0)
                            ?.requestFocus()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Channel load failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }
}
