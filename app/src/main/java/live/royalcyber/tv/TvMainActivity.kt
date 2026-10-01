package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TvMainActivity : AppCompatActivity() {

    companion object {
        private const val CHANNELS_URL =
            "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"
    }

    private lateinit var channelListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView

    private val channelNames = ArrayList<String>()
    private val channelLogos = ArrayList<String>()
    private val channelUrls = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tv_main)

        channelListView = findViewById(R.id.tv_channel_list)
        progressBar = findViewById(R.id.tv_progress)
        emptyText = findViewById(R.id.tv_empty_text)

        setupChannelList()
        loadChannels()
    }

    private fun setupChannelList() {

        channelListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->

                if (position < 0 || position >= channelUrls.size) {
                    return@OnItemClickListener
                }

                val name = channelNames[position]
                val logo = channelLogos[position]
                val streamUrl = channelUrls[position]

                if (streamUrl.isBlank()) {
                    Toast.makeText(
                        this,
                        "এই Channel-এর stream পাওয়া যায়নি",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnItemClickListener
                }

                val intent = Intent(this, TvPlayerActivity::class.java)

                intent.putExtra("channel_name", name)
                intent.putExtra("channel_logo", logo)
                intent.putExtra("stream_url", streamUrl)

                startActivity(intent)
            }
    }

    private fun loadChannels() {

        showLoading(true)

        thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(CHANNELS_URL)

                connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.useCaches = false

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP $responseCode")
                }

                val inputStream = connection.inputStream

                val reader = BufferedReader(
                    InputStreamReader(inputStream, Charsets.UTF_8)
                )

                val jsonText = reader.use { it.readText() }

                parseChannels(jsonText)

                runOnUiThread {

                    showLoading(false)

                    if (channelNames.isEmpty()) {

                        showEmpty(true)

                    } else {

                        showEmpty(false)
                        showChannelList()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    showLoading(false)
                    showEmpty(true)

                    Toast.makeText(
                        this,
                        "Channel load failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }

    private fun parseChannels(jsonText: String) {

        channelNames.clear()
        channelLogos.clear()
        channelUrls.clear()

        try {

            val jsonArray = JSONArray(jsonText)

            for (i in 0 until jsonArray.length()) {

                val item = jsonArray.optJSONObject(i)
                    ?: continue

                val name = item.optString("name").trim()
                val logo = item.optString("logo").trim()
                val streamUrl = item.optString("streamUrl").trim()

                if (name.isNotEmpty() && streamUrl.isNotEmpty()) {

                    channelNames.add(name)
                    channelLogos.add(logo)
                    channelUrls.add(streamUrl)
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    private fun showChannelList() {

        val adapter = ArrayAdapter(
            this,
            R.layout.tv_channel_item,
            R.id.tv_channel_name,
            channelNames
        )

        channelListView.adapter = adapter

        channelListView.visibility = View.VISIBLE

        if (channelNames.isNotEmpty()) {
            channelListView.requestFocus()
            channelListView.setSelection(0)
        }
    }

    private fun showLoading(show: Boolean) {

        progressBar.visibility =
            if (show) View.VISIBLE else View.GONE

        if (show) {
            channelListView.visibility = View.GONE
            emptyText.visibility = View.GONE
        }
    }

    private fun showEmpty(show: Boolean) {

        emptyText.visibility =
            if (show) View.VISIBLE else View.GONE

        if (show) {
            channelListView.visibility = View.GONE
        }
    }

    override fun onBackPressed() {

        super.onBackPressed()
    }
}
