package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class TvMainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView

    private val channels =
        ArrayList<TvChannel>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_tv_main
        )

        recyclerView =
            findViewById(
                R.id.tv_channel_list
            )

        progressBar =
            findViewById(
                R.id.tv_progress
            )

        emptyText =
            findViewById(
                R.id.tv_empty
            )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        loadChannels()
    }

    private fun loadChannels() {

        progressBar.visibility =
            View.VISIBLE

        emptyText.visibility =
            View.GONE

        try {

            /*
             * ==========================================
             * LOCAL TV CHANNEL JSON
             *
             * assets/tv_channels.json
             * ==========================================
             */

            val response =
                assets.open(
                    "tv_channels.json"
                )
                    .bufferedReader(
                        Charsets.UTF_8
                    )
                    .use {
                        it.readText()
                    }

            if (response.isBlank()) {

                throw Exception(
                    "tv_channels.json is empty"
                )
            }

            val jsonArray =
                JSONArray(response)

            val result =
                ArrayList<TvChannel>()

            for (
                i in 0 until jsonArray.length()
            ) {

                val item =
                    jsonArray.optJSONObject(i)
                        ?: continue

                val name =
                    item.optString(
                        "name",
                        ""
                    ).trim()

                val logo =
                    item.optString(
                        "logo",
                        ""
                    ).trim()

                val streamUrl =
                    item.optString(
                        "streamUrl",
                        ""
                    ).trim()

                if (
                    name.isNotEmpty() &&
                    streamUrl.isNotEmpty()
                ) {

                    result.add(
                        TvChannel(
                            name = name,
                            logo = logo,
                            streamUrl = streamUrl
                        )
                    )
                }
            }

            progressBar.visibility =
                View.GONE

            channels.clear()
            channels.addAll(result)

            if (channels.isEmpty()) {

                emptyText.visibility =
                    View.VISIBLE

                emptyText.text =
                    "No channels available"

                return

            }

            emptyText.visibility =
                View.GONE

            recyclerView.adapter =
                TvChannelAdapter(
                    channels
                ) { channel ->

                    openChannel(channel)
                }

            /*
             * ==========================================
             * FIRST CHANNEL FOCUS
             * ==========================================
             */

            recyclerView.post {

                if (
                    recyclerView.childCount > 0
                ) {

                    recyclerView
                        .getChildAt(0)
                        ?.requestFocus()
                }
            }

        } catch (e: Exception) {

            progressBar.visibility =
                View.GONE

            emptyText.visibility =
                View.VISIBLE

            emptyText.text =
                "Channels could not be loaded"
        }
    }

    private fun openChannel(
        channel: TvChannel
    ) {

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
            channel.streamUrl
        )

        intent.putExtra(
            "channel_logo",
            channel.logo
        )

        startActivity(intent)
    }

    data class TvChannel(
        val name: String,
        val logo: String,
        val streamUrl: String
    )

    private class TvChannelAdapter(
        private val list: List<TvChannel>,
        private val onClick:
            (TvChannel) -> Unit
    ) : RecyclerView.Adapter<
            TvChannelAdapter.ChannelHolder>() {

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): ChannelHolder {

            val view =
                android.view.LayoutInflater
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

            holder.bind(
                list[position]
            )
        }

        override fun getItemCount(): Int {

            return list.size
        }

        inner class ChannelHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(
            itemView
        ) {

            private val nameText:
                TextView =
                itemView.findViewById(
                    R.id.channel_name
                )

            fun bind(
                channel: TvChannel
            ) {

                nameText.text =
                    channel.name

                /*
                 * Remote / OK
                 */

                itemView.setOnClickListener {

                    onClick(channel)
                }

                itemView.setOnKeyListener {
                        _,
                        keyCode,
                        event ->

                    if (
                        event.action ==
                            KeyEvent.ACTION_UP &&
                        (
                            keyCode ==
                                KeyEvent.KEYCODE_DPAD_CENTER ||
                            keyCode ==
                                KeyEvent.KEYCODE_ENTER
                        )
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
