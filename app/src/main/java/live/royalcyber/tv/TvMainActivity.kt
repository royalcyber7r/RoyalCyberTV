package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

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

    private var channels: List<Channel> = emptyList()

    private val channelsJsonUrl =
        "https://raw.githubusercontent.com/royalcyber7r/RoyalCyberTV/main/assets/channels.json"


    companion object {

        const val EXTRA_CHANNEL_NAME =
            "tv_channel_name"

        const val EXTRA_CHANNEL_LOGO =
            "tv_channel_logo"

        const val EXTRA_CHANNEL_URL =
            "tv_channel_url"
    }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * TV Main-এ শুধু Channel List থাকবে।
         *
         * Player এখানে তৈরি হবে না।
         *
         * Flow:
         *
         * TvMainActivity
         *      ↓
         * Channel List
         *      ↓
         * Channel Select
         *      ↓
         * TvPlayerActivity
         */

        setContentView(
            R.layout.activity_tv_main
        )


        channelList =
            findViewById(
                R.id.tv_channel_list
            )

        loadingText =
            findViewById(
                R.id.tv_loading
            )


        /*
         * Remote / D-Pad navigation-এর জন্য
         * RecyclerView focusable রাখা হচ্ছে।
         */
        channelList.isFocusable = true

        channelList.isFocusableInTouchMode = true


        /*
         * Simple vertical TV Channel List
         */
        channelList.layoutManager =
            LinearLayoutManager(this)


        /*
         * JSON থেকে Channel load
         */
        loadChannels()
    }


    /* =========================================================
       LOAD CHANNELS
       ========================================================= */

    private fun loadChannels() {

        showLoading(
            "Loading Channels..."
        )


        thread {

            var connection:
                    HttpURLConnection? = null

            try {

                val url =
                    URL(channelsJsonUrl)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    15000

                connection.useCaches =
                    false

                connection.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
                )


                val responseCode =
                    connection.responseCode


                if (
                    responseCode !in 200..299
                ) {

                    throw Exception(
                        "HTTP $responseCode"
                    )
                }


                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                val jsonArray =
                    JSONArray(response)


                val loadedChannels =
                    mutableListOf<Channel>()


                for (
                    index in 0 until jsonArray.length()
                ) {

                    val item =
                        jsonArray.optJSONObject(
                            index
                        )
                            ?: continue


                    val name =
                        item
                            .optString("name")
                            .trim()


                    val logo =
                        item
                            .optString("logo")
                            .trim()


                    val streamUrl =
                        item
                            .optString("streamUrl")
                            .trim()


                    /*
                     * Name এবং Stream URL থাকলেই
                     * Channel valid ধরা হবে।
                     */
                    if (
                        name.isNotEmpty() &&
                        streamUrl.isNotEmpty()
                    ) {

                        loadedChannels.add(
                            Channel(
                                name = name,
                                logo = logo,
                                streamUrl = streamUrl
                            )
                        )
                    }
                }


                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    channels =
                        loadedChannels


                    if (
                        channels.isEmpty()
                    ) {

                        showError(
                            "কোনো Channel পাওয়া যায়নি"
                        )

                        return@runOnUiThread
                    }


                    showChannels()
                }


            } catch (
                e: Exception
            ) {

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    showError(
                        "Channel loading failed"
                    )


                    Toast.makeText(
                        this,
                        "Channels লোড করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } finally {

                try {

                    connection?.disconnect()

                } catch (
                    _: Exception
                ) {
                }
            }
        }
    }


    /* =========================================================
       SHOW CHANNEL LIST
       ========================================================= */

    private fun showChannels() {

        loadingText.visibility =
            View.GONE


        /*
         * আপনার existing ChannelAdapter ব্যবহার করা হবে।
         *
         * Channel select করলে সরাসরি
         * TvPlayerActivity open হবে।
         */
        val adapter =
            ChannelAdapter(
                channels = channels,
                onChannelClick = { channel ->

                    openTvPlayer(
                        channel
                    )
                }
            )


        channelList.adapter =
            adapter


        /*
         * প্রথম Channel-এ TV remote focus
         */
        channelList.post {

            if (
                channels.isNotEmpty() &&
                !isFinishing &&
                !isDestroyed
            ) {

                channelList.scrollToPosition(
                    0
                )


                channelList.requestFocus()


                channelList
                    .layoutManager
                    ?.findViewByPosition(0)
                    ?.requestFocus()
            }
        }
    }


    /* =========================================================
       OPEN TV PLAYER
       ========================================================= */

    private fun openTvPlayer(
        channel: Channel
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }


        val streamUrl =
            channel.streamUrl.trim()


        if (streamUrl.isEmpty()) {

            Toast.makeText(
                this,
                "এই Channel-এর Stream URL নেই",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        /*
         * TvPlayerActivity-তে Channel information
         * Intent extras দিয়ে পাঠানো হবে।
         */
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


    /* =========================================================
       LOADING
       ========================================================= */

    private fun showLoading(
        message: String
    ) {

        if (
            !::loadingText.isInitialized
        ) {
            return
        }


        loadingText.text =
            message


        loadingText.visibility =
            View.VISIBLE
    }


    /* =========================================================
       ERROR
       ========================================================= */

    private fun showError(
        message: String
    ) {

        if (
            !::loadingText.isInitialized
        ) {
            return
        }


        loadingText.text =
            message


        loadingText.visibility =
            View.VISIBLE
    }


    /* =========================================================
       RESUME
       ========================================================= */

    override fun onResume() {

        super.onResume()


        /*
         * TvPlayerActivity থেকে Back করে
         * Channel List-এ ফিরলে আবার focus থাকবে।
         */
        if (
            ::channelList.isInitialized
        ) {

            channelList.post {

                if (
                    !isFinishing &&
                    !isDestroyed
                ) {

                    channelList.requestFocus()
                }
            }
        }
    }


    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        /*
         * কোনো Player নেই।
         * তাই এখানে ExoPlayer release করার দরকার নেই।
         */

        if (
            ::channelList.isInitialized
        ) {

            channelList.adapter =
                null
        }


        super.onDestroy()
    }
}
