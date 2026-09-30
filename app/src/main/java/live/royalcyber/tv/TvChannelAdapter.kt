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

class TvChannelAdapter(
    private val channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<TvChannelAdapter.ViewHolder>() {

    private val executor =
        Executors.newFixedThreadPool(3)

    private val handler =
        Handler(Looper.getMainLooper())

    class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val logo: ImageView =
            view.findViewById(R.id.tv_channel_logo)

        val name: TextView =
            view.findViewById(R.id.tv_channel_name)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_tv_channel,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val channel =
            channels[position]

        holder.name.text =
            channel.name

        holder.logo.setImageResource(
            android.R.drawable.sym_def_app_icon
        )

        val logoUrl =
            channel.logo.trim()

        if (logoUrl.isNotEmpty()) {

            loadLogo(
                logoUrl,
                holder.logo
            )
        }

        holder.itemView.setOnClickListener {
            onChannelClick(channel)
        }
    }

    private fun loadLogo(
        logoUrl: String,
        imageView: ImageView
    ) {

        executor.execute {

            try {

                val connection =
                    URL(logoUrl)
                        .openConnection()
                            as HttpURLConnection

                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val bitmap =
                    connection.inputStream.use {
                        BitmapFactory.decodeStream(it)
                    }

                connection.disconnect()

                if (bitmap != null) {

                    handler.post {

                        if (!imageView.isAttachedToWindow) {
                            return@post
                        }

                        imageView.setImageBitmap(bitmap)
                    }
                }

            } catch (_: Exception) {
                // Default icon remains
            }
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
