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

            if (isDownloading) {
                return@setOnClickListener
            }

            if (apkUrl.isNotEmpty()) {

                startDownload()

            } else {

                checkLatestRelease()
            }
        }


        laterText.setOnClickListener {

            finish()
        }


        checkLatestRelease()
    }


    private fun checkLatestRelease() {

        updateButton.isEnabled = false

        updateButton.text =
            "Checking Update..."


        executor.execute {

            var connection: HttpURLConnection? = null

            try {

                val apiUrl =
                    URL(GITHUB_API)

                connection =
                    apiUrl.openConnection()
                            as HttpURLConnection


                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    20000

                connection.readTimeout =
                    20000

                connection.useCaches =
                    false

                connection.instanceFollowRedirects =
                    true


                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV Android App"
                )

                connection.setRequestProperty(
                    "X-GitHub-Api-Version",
                    "2022-11-28"
                )


                connection.connect()


                val responseCode =
                    connection.responseCode


                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    throw Exception(
                        "GitHub API HTTP $responseCode"
                    )
                }


                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                if (response.isBlank()) {

                    throw Exception(
                        "GitHub API থেকে কোনো response পাওয়া যায়নি"
                    )
                }


                val json =
                    JSONObject(response)


                val tagName =
                    json.optString(
                        "tag_name",
                        ""
                    ).trim()


                val releaseName =
                    json.optString(
                        "name",
                        ""
                    ).trim()


                val releaseBody =
                    json.optString(
                        "body",
                        ""
                    ).trim()


                if (tagName.isEmpty()) {

                    throw Exception(
                        "Latest Release-এর tag পাওয়া যায়নি"
                    )
                }


                /*
                 * GitHub Release থেকে RoyalCyberTV.apk খোঁজা
                 */

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
                            ).trim()


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
                                ).trim()

                            break
                        }
                    }
                }


                if (downloadUrl.isEmpty()) {

                    throw Exception(
                        "Latest Release-এ $APK_NAME পাওয়া যায়নি"
                    )
                }


                /*
                 * Release Version
                 */

                val latestVersionCode =
                    extractVersionCode(
                        tagName
                    )


                /*
                 * Installed App Version
                 */

                val currentVersionCode =
                    getCurrentVersionCode()


                if (latestVersionCode <= 0) {

                    throw Exception(
                        "Release version বুঝতে পারিনি: $tagName"
                    )
                }


                handler.post {

                    if (
                        latestVersionCode >
                        currentVersionCode
                    ) {

                        apkUrl =
                            downloadUrl


                        versionText.text =
                            "Version ${formatVersion(tagName)} is now available"


                        messageText.text =
                            when {

                                releaseBody.isNotBlank() ->
                                    releaseBody

                                releaseName.isNotBlank() ->
                                    releaseName

                                else ->
                                    "New version is available with important fixes. Please update now to continue watching."
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


            } catch (
                e: Exception
            ) {

                handler.post {

                    apkUrl = ""

                    updateButton.text =
                        "Try Again"

                    updateButton.isEnabled =
                        true


                    val errorMessage =
                        e.message
                            ?: "Unknown error"


                    Toast.makeText(
                        this@UpdateActivity,
                        "Update check করা যাচ্ছে না\n$errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                connection?.disconnect()
            }
        }
    }


    private fun startDownload() {

        if (isDownloading) {
            return
        }


        if (apkUrl.isEmpty()) {

            checkLatestRelease()

            return
        }


        /*
         * Android 8+ Unknown Sources Permission
         */

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

            var connection: HttpURLConnection? = null

            try {

                connection =
                    URL(apkUrl)
                        .openConnection()
                            as HttpURLConnection


                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    20000

                connection.readTimeout =
                    60000

                connection.useCaches =
                    false

                connection.instanceFollowRedirects =
                    true


                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV Android App"
                )


                connection.connect()


                val responseCode =
                    connection.responseCode


                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    throw Exception(
                        "APK Download HTTP $responseCode"
                    )
                }


                val totalBytes =
                    connection.contentLengthLong


                val downloadDirectory =
                    getExternalFilesDir(
                        "Download"
                    )


                if (
                    downloadDirectory == null
                ) {

                    throw Exception(
                        "Download folder পাওয়া যায়নি"
                    )
                }


                if (
                    !downloadDirectory.exists()
                ) {

                    downloadDirectory.mkdirs()
                }


                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                if (apkFile.exists()) {

                    apkFile.delete()
                }


                connection.inputStream.use { input ->

                    apkFile.outputStream().use { output ->

                        val buffer =
                            ByteArray(8192)

                        var downloaded =
                            0L


                        while (true) {

                            val read =
                                input.read(
                                    buffer
                                )


                            if (read == -1) {
                                break
                            }


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
                    }
                }


                if (
                    !apkFile.exists() ||
                    apkFile.length() <= 0
                ) {

                    throw Exception(
                        "Downloaded APK file invalid"
                    )
                }


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


                    val errorMessage =
                        e.message
                            ?: "Unknown error"


                    Toast.makeText(
                        this@UpdateActivity,
                        "APK Download করা যায়নি\n$errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } finally {

                connection?.disconnect()
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

            isDownloading =
                false


            updateButton.isEnabled =
                true

            updateButton.text =
                "Try Again"


            Toast.makeText(
                this,
                "APK Install করা যাচ্ছে না\n${e.message ?: ""}",
                Toast.LENGTH_LONG
            ).show()
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


    /*
     * GitHub Release Tag থেকে Version Code বের করা
     *
     * build-125  -> 125
     * build-126  -> 126
     * v1.0.2     -> 2
     * 1.0.2      -> 2
     */

    private fun extractVersionCode(
        tag: String
    ): Int {

        val cleanTag =
            tag.trim()


        /*
         * build-125 / build_125 / build 125
         */

        val buildRegex =
            Regex(
                "(?i)build[-_ ]?(\\d+)"
            )


        val buildMatch =
            buildRegex.find(
                cleanTag
            )


        if (buildMatch != null) {

            return buildMatch
                .groupValues[1]
                .toIntOrNull()
                ?: 0
        }


        /*
         * v1.0.2 / 1.0.2
         */

        val versionRegex =
            Regex(
                "^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)"
            )


        val versionMatch =
            versionRegex.find(
                cleanTag
            )


        if (versionMatch != null) {

            return versionMatch
                .groupValues[3]
                .toIntOrNull()
                ?: 0
        }


        /*
         * অন্য কোনো tag হলে শেষের সংখ্যা নেওয়া
         */

        val numberRegex =
            Regex(
                "(\\d+)"
            )


        val matches =
            numberRegex.findAll(
                cleanTag
            ).toList()


        if (matches.isNotEmpty()) {

            return matches
                .last()
                .value
                .toIntOrNull()
                ?: 0
        }


        return 0
    }


    private fun formatVersion(
        tag: String
    ): String {

        val cleanTag =
            tag.trim()


        if (
            cleanTag.startsWith(
                "build-",
                ignoreCase = true
            )
        ) {

            return cleanTag
        }


        return cleanTag
            .removePrefix("v")
            .removePrefix("V")
            .ifEmpty {
                "1.0.0"
            }
    }


    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )


        executor.shutdownNow()


        super.onDestroy()
    }
}
