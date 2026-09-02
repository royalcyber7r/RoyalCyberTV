package live.royalcyber.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Channel(
    val name: String,
    val logo: String,
    val streamUrl: String
)

class ChannelAdapter(
    private var channels: List<Channel>,
    private val onChannelClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val logo: ImageView =
            itemView.findViewById(R.id.channel_logo)

        val name: TextView =
            itemView.findViewById(R.id.channel_name)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChannelViewHolder {

        val view = LayoutInflater.from(parent.context)
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

        val channel = channels[position]

        holder.name.text = channel.name

        holder.logo.setImageResource(
            android.R.drawable.sym_def_app_icon
        )

        holder.itemView.setOnClickListener {
            onChannelClick(channel)
        }
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    fun updateList(newList: List<Channel>) {
        channels = newList
        notifyDataSetChanged()
    }
}
