package live.royalcyber.tv

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

import org.json.JSONObject

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


class UpdateActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var versionText: TextView
    private lateinit var messageText: TextView
    private lateinit var updateButton: Button
    private lateinit var laterText: TextView

    private val executor =
        Executors.newSingleThreadExecutor()

    private val handler =
        Handler(Looper.getMainLooper())

    private var apkUrl = ""

    private var isDownloading = false


    companion object {

        private const val GITHUB_API =
            "https://api.github.com/repos/royalcyber7r/RoyalCyberTV/releases/latest"

        private const val APK_NAME =
            "RoyalCyberTV.apk"
    }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_update
        )

        progressBar =
            findViewById(
                R.id.update_progress
            )

        progressText =
            findViewById(
                R.id.update_percent
            )

        versionText =
            findViewById(
                R.id.update_version
            )

        messageText =
            findViewById(
                R.id.update_message
            )

        updateButton =
            findViewById(
                R.id.update_button
            )

        laterText =
            findViewById(
                R.id.update_later
            )


        updateButton.setOnClickListener {

            if (!isDownloading) {

                if (apkUrl.isNotEmpty()) {

                    startDownload()

                } else {

                    checkLatestRelease()
                }
            }
        }


        laterText.setOnClickListener {

            finish()
        }


        checkLatestRelease()
    }


    private fun checkLatestRelease() {

        updateButton.isEnabled =
            false

        updateButton.text =
            "Checking Update..."


        executor.execute {

            try {

                val url =
                    URL(GITHUB_API)

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    throw Exception(
                        "GitHub response: $responseCode"
                    )
                }


                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                val json =
                    JSONObject(response)

                val tagName =
                    json.optString(
                        "tag_name",
                        ""
                    )

                val releaseName =
                    json.optString(
                        "name",
                        ""
                    )

                val releaseBody =
                    json.optString(
                        "body",
                        ""
                    )


                var downloadUrl =
                    ""


                val assets =
                    json.optJSONArray(
                        "assets"
                    )


                if (assets != null) {

                    for (
                        i in 0 until assets.length()
                    ) {

                        val asset =
                            assets.getJSONObject(i)

                        val assetName =
                            asset.optString(
                                "name",
                                ""
                            )

                        if (
                            assetName.equals(
                                APK_NAME,
                                ignoreCase = true
                            )
                        ) {

                            downloadUrl =
                                asset.optString(
                                    "browser_download_url",
                                    ""
                                )

                            break
                        }
                    }
                }


                val latestVersionCode =
                    extractVersionCode(
                        tagName
                    )


                val currentVersionCode =
                    getCurrentVersionCode()


                handler.post {

                    if (
                        latestVersionCode >
                        currentVersionCode &&
                        downloadUrl.isNotEmpty()
                    ) {

                        apkUrl =
                            downloadUrl

                        versionText.text =
                            "Version ${formatVersion(tagName)} is now available"

                        messageText.text =
                            if (
                                releaseBody.isNotBlank()
                            ) {
                                releaseBody
                            } else {
                                "New version is now available with important fixes. Please update now to continue watching."
                            }

                        updateButton.text =
                            "Update Now"

                        updateButton.isEnabled =
                            true

                    } else {

                        Toast.makeText(
                            this@UpdateActivity,
                            "আপনার App সর্বশেষ Version-এ আছে",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                }


                connection.disconnect()

            } catch (
                e: Exception
            ) {

                handler.post {

                    updateButton.text =
                        "Try Again"

                    updateButton.isEnabled =
                        true

                    Toast.makeText(
                        this@UpdateActivity,
                        "Update check করা যাচ্ছে না",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun startDownload() {

        if (isDownloading) {
            return
        }

        if (apkUrl.isEmpty()) {
            return
        }


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            if (
                !packageManager
                    .canRequestPackageInstalls()
            ) {

                try {

                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse(
                                "package:$packageName"
                            )
                        )

                    startActivity(intent)

                    Toast.makeText(
                        this,
                        "Install permission চালু করে আবার Update চাপুন",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (
                    _: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Install permission চালু করুন",
                        Toast.LENGTH_LONG
                    ).show()
                }

                return
            }
        }


        isDownloading =
            true

        updateButton.isEnabled =
            false

        updateButton.text =
            "Downloading..."


        progressBar.progress =
            0

        progressText.text =
            "0 %"


        executor.execute {

            try {

                val connection =
                    URL(apkUrl)
                        .openConnection()
                            as HttpURLConnection

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    30000

                connection.instanceFollowRedirects =
                    true

                connection.connect()


                val totalBytes =
                    connection.contentLengthLong


                val apkFile =
                    File(
                        getExternalFilesDir(
                            "Download"
                        ),
                        APK_NAME
                    )


                if (apkFile.exists()) {
                    apkFile.delete()
                }


                val input =
                    connection.inputStream

                val output =
                    apkFile.outputStream()


                val buffer =
                    ByteArray(8192)

                var downloaded =
                    0L

                var read: Int


                while (
                    input.read(buffer)
                        .also {
                            read = it
                        } != -1
                ) {

                    output.write(
                        buffer,
                        0,
                        read
                    )

                    downloaded +=
                        read.toLong()


                    if (
                        totalBytes > 0
                    ) {

                        val percent =
                            (
                                downloaded *
                                    100L /
                                    totalBytes
                                ).toInt()

                        handler.post {

                            progressBar.progress =
                                percent

                            progressText.text =
                                "$percent %"
                        }
                    }
                }


                output.flush()
                output.close()
                input.close()

                connection.disconnect()


                handler.post {

                    progressBar.progress =
                        100

                    progressText.text =
                        "100 %"

                    updateButton.text =
                        "Installing..."

                    installApk(
                        apkFile
                    )
                }

            } catch (
                e: Exception
            ) {

                handler.post {

                    isDownloading =
                        false

                    updateButton.isEnabled =
                        true

                    updateButton.text =
                        "Try Again"

                    Toast.makeText(
                        this@UpdateActivity,
                        "APK Download করা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun installApk(
        apkFile: File
    ) {

        try {

            val apkUri =
                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    apkFile
                )


            val intent =
                Intent(
                    Intent.ACTION_VIEW
                )

            intent.setDataAndType(
                apkUri,
                "application/vnd.android.package-archive"
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "APK Install করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()

            isDownloading =
                false

            updateButton.isEnabled =
                true

            updateButton.text =
                "Try Again"
        }
    }


    private fun getCurrentVersionCode(): Int {

        return try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                packageManager
                    .getPackageInfo(
                        packageName,
                        0
                    )
                    .longVersionCode
                    .toInt()

            } else {

                @Suppress("DEPRECATION")

                packageManager
                    .getPackageInfo(
                        packageName,
                        0
                    )
                    .versionCode
            }

        } catch (
            _: Exception
        ) {

            1
        }
    }


    private fun extractVersionCode(
        tag: String
    ): Int {

        val number =
            tag.filter {
                it.isDigit()
            }

        return number.toIntOrNull()
            ?: 0
    }


    private fun formatVersion(
        tag: String
    ): String {

        val code =
            extractVersionCode(tag)

        return "1.0.$code"
    }


    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        executor.shutdownNow()

        super.onDestroy()
    }
}
