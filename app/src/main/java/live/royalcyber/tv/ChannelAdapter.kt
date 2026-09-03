package live.royalcyber.tv

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class Channel(
    val name: String,
    val logo: String,
    val streamUrl: String
)

class ChannelAdapter(
    private var channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private val executor = Executors.newFixedThreadPool(4)

    private val mainHandler =
        Handler(Looper.getMainLooper())

    class ChannelViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val logo: ImageView =
            itemView.findViewById(R.id.channel_logo)

        val name: TextView =
            itemView.findViewById(R.id.channel_name)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChannelViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_channel,
                    parent,
                    false
                )

        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ChannelViewHolder,
        position: Int
    ) {

        val channel =
            channels[position]

        /*
         * Channel name hidden.
         * Every channel shows LIVE.
         */
        holder.name.text = "🔴 LIVE"

        holder.logo.setImageResource(
            android.R.drawable.sym_def_app_icon
        )

        holder.logo.tag = channel.logo

        loadImage(
            channel.logo,
            holder.logo
        )

        holder.itemView.setOnClickListener {

            onChannelClick(channel)
        }
    }

    private fun loadImage(
        imageUrl: String,
        imageView: ImageView
    ) {

        executor.execute {

            try {

                val url =
                    URL(imageUrl)

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doInput = true

                connection.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        connection.inputStream
                    )

                connection.disconnect()

                if (bitmap != null) {

                    mainHandler.post {

                        if (imageView.tag == imageUrl) {

                            imageView.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                }

            } catch (_: Exception) {

                mainHandler.post {

                    if (imageView.tag == imageUrl) {

                        imageView.setImageResource(
                            android.R.drawable
                                .sym_def_app_icon
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    fun updateList(
        newList: List<Channel>
    ) {

        channels = newList

        notifyDataSetChanged()
    }

    fun shutdown() {

        executor.shutdownNow()
    }
}
