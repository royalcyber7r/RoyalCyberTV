package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TvMainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView

    private val channels = ArrayList<TvChannel>()

    private val channelsUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_main)

        recyclerView = findViewById(R.id.tv_channel_list)
        progressBar = findViewById(R.id.tv_progress)
        emptyText = findViewById(R.id.tv_empty)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadChannels()
    }

    private fun loadChannels() {

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        thread {

            try {

                val connection =
                    URL(channelsUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val jsonArray = JSONArray(response)

                val result = ArrayList<TvChannel>()

                for (i in 0 until jsonArray.length()) {

                    val item = jsonArray.optJSONObject(i)
                        ?: continue

                    val name =
                        item.optString("name")
                            .ifEmpty {
                                item.optString("title")
                            }

                    val url =
                        item.optString("url")
                            .ifEmpty {
                                item.optString("stream")
                            }
                            .ifEmpty {
                                item.optString("stream_url")
                            }

                    val logo =
                        item.optString("logo")
                            .ifEmpty {
                                item.optString("logo_url")
                            }

                    if (name.isNotEmpty() && url.isNotEmpty()) {

                        result.add(
                            TvChannel(
                                name = name,
                                url = url,
                                logo = logo
                            )
                        )
                    }
                }

                runOnUiThread {

                    progressBar.visibility = View.GONE

                    channels.clear()
                    channels.addAll(result)

                    if (channels.isEmpty()) {

                        emptyText.visibility = View.VISIBLE

                    } else {

                        emptyText.visibility = View.GONE

                        recyclerView.adapter =
                            TvChannelAdapter(channels) { channel ->

                                val intent =
                                    Intent(
                                        this,
                                        TvPlayerActivity::class.java
                                    )

                                intent.putExtra(
                                    "channel_name",
                                    channel.name
                                )

                                intent.putExtra(
                                    "channel_url",
                                    channel.url
                                )

                                startActivity(intent)
                            }

                        recyclerView.post {

                            recyclerView.getChildAt(0)
                                ?.requestFocus()
                        }
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    progressBar.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "Channels could not be loaded"
                }
            }
        }
    }

    data class TvChannel(
        val name: String,
        val url: String,
        val logo: String
    )

    private class TvChannelAdapter(
        private val list: List<TvChannel>,
        private val onClick: (TvChannel) -> Unit
    ) : RecyclerView.Adapter<TvChannelAdapter.ChannelHolder>() {

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): ChannelHolder {

            val view = android.view.LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.tv_channel_item,
                    parent,
                    false
                )

            return ChannelHolder(view)
        }

        override fun onBindViewHolder(
            holder: ChannelHolder,
            position: Int
        ) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int {
            return list.size
        }

        inner class ChannelHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            private val nameText: TextView =
                itemView.findViewById(R.id.channel_name)

            fun bind(channel: TvChannel) {

                nameText.text = channel.name

                itemView.setOnClickListener {
                    onClick(channel)
                }

                itemView.setOnKeyListener { _, keyCode, event ->

                    if (
                        event.action ==
                        android.view.KeyEvent.ACTION_UP &&
                        keyCode ==
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER
                    ) {
                        onClick(channel)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}
