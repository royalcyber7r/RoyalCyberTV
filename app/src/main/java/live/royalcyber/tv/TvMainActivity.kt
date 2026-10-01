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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
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

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        loadChannels()
    }

    private fun loadChannels() {

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(channelsUrl)

                connection =
                    url.openConnection() as HttpURLConnection

                if (connection is HttpsURLConnection) {

                    connection.sslSocketFactory =
                        createTls12SocketFactory()
                }

                connection.requestMethod = "GET"

                connection.connectTimeout = 20000
                connection.readTimeout = 30000

                connection.instanceFollowRedirects = true

                connection.useCaches = false
                connection.defaultUseCaches = false

                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android 7.0; RoyalCyberTV)"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json,text/plain,*/*"
                )

                connection.connect()

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception(
                        "HTTP $responseCode"
                    )
                }

                val reader =
                    BufferedReader(
                        InputStreamReader(
                            connection.inputStream,
                            Charsets.UTF_8
                        )
                    )

                val response =
                    reader.use {
                        it.readText()
                    }

                if (response.trim().isEmpty()) {
                    throw Exception("Empty JSON")
                }

                val jsonArray =
                    JSONArray(response)

                val result =
                    ArrayList<TvChannel>()

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
                            TvChannel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }

                runOnUiThread {

                    progressBar.visibility =
                        View.GONE

                    channels.clear()
                    channels.addAll(result)

                    if (channels.isEmpty()) {

                        emptyText.visibility =
                            View.VISIBLE

                        emptyText.text =
                            "No channels available"

                    } else {

                        emptyText.visibility =
                            View.GONE

                        recyclerView.adapter =
                            TvChannelAdapter(
                                channels
                            ) { channel ->

                                openChannel(channel)
                            }

                        recyclerView.post {

                            if (
                                recyclerView.childCount > 0
                            ) {

                                recyclerView
                                    .getChildAt(0)
                                    ?.requestFocus()
                            }
                        }
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    progressBar.visibility =
                        View.GONE

                    emptyText.visibility =
                        View.VISIBLE

                    emptyText.text =
                        "Channels could not be loaded"
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    private fun createTls12SocketFactory(): SSLSocketFactory {

        val sslContext =
            SSLContext.getInstance("TLS")

        sslContext.init(
            null,
            null,
            null
        )

        return Tls12SocketFactory(
            sslContext.socketFactory
        )
    }

    private class Tls12SocketFactory(
        private val delegate: SSLSocketFactory
    ) : SSLSocketFactory() {

        override fun getDefaultCipherSuites(): Array<String> =
            delegate.defaultCipherSuites

        override fun getSupportedCipherSuites(): Array<String> =
            delegate.supportedCipherSuites

        override fun createSocket(
            socket: java.net.Socket?,
            host: String?,
            port: Int,
            autoClose: Boolean
        ): SSLSocket {

            return enableTls(
                delegate.createSocket(
                    socket,
                    host,
                    port,
                    autoClose
                )
            )
        }

        override fun createSocket(
            host: String?,
            port: Int
        ): SSLSocket {

            return enableTls(
                delegate.createSocket(
                    host,
                    port
                )
            )
        }

        override fun createSocket(
            host: String?,
            port: Int,
            localHost: java.net.InetAddress?,
            localPort: Int
        ): SSLSocket {

            return enableTls(
                delegate.createSocket(
                    host,
                    port,
                    localHost,
                    localPort
                )
            )
        }

        override fun createSocket(
            host: java.net.InetAddress?,
            port: Int
        ): SSLSocket {

            return enableTls(
                delegate.createSocket(
                    host,
                    port
                )
            )
        }

        override fun createSocket(
            address: java.net.InetAddress?,
            port: Int,
            localAddress: java.net.InetAddress?,
            localPort: Int
        ): SSLSocket {

            return enableTls(
                delegate.createSocket(
                    address,
                    port,
                    localAddress,
                    localPort
                )
            )
        }

        private fun enableTls(
            socket: SSLSocket
        ): SSLSocket {

            socket.enabledProtocols =
                arrayOf("TLSv1.2")

            return socket
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
        private val onClick: (TvChannel) -> Unit
    ) : RecyclerView.Adapter<TvChannelAdapter.ChannelHolder>() {

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

            holder.bind(list[position])
        }

        override fun getItemCount(): Int {

            return list.size
        }

        inner class ChannelHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            private val nameText: TextView =
                itemView.findViewById(
                    R.id.channel_name
                )

            fun bind(
                channel: TvChannel
            ) {

                nameText.text =
                    channel.name

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
                        keyCode ==
                            KeyEvent.KEYCODE_DPAD_CENTER
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
