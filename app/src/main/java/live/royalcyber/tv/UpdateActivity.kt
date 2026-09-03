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

import org.json.JSONArray
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

        /*
         * GitHub Releases List API
         *
         * Private repository নয়,
         * তাই Public repository থেকে কাজ করবে।
         */
        private const val GITHUB_API =
            "https://api.github.com/repos/royalcyber7r/RoyalCyberTV/releases?per_page=100"

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


    /*
     * =========================================================
     * CHECK LATEST RELEASE
     * =========================================================
     */

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


                /*
                 * GitHub API Headers
                 */

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
                        "GitHub Releases API HTTP $responseCode"
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
                        "GitHub থেকে কোনো response পাওয়া যায়নি"
                    )
                }


                /*
                 * JSON Array
                 */

                val releases =
                    JSONArray(response)


                if (releases.length() == 0) {

                    throw Exception(
                        "GitHub-এ কোনো Release পাওয়া যায়নি"
                    )
                }


                /*
                 * =================================================
                 * সবচেয়ে বড় BUILD NUMBER-এর RELEASE নির্বাচন করা হবে
                 * =================================================
                 */

                var selectedRelease: JSONObject? =
                    null

                var selectedTag =
                    ""

                var selectedName =
                    ""

                var selectedBody =
                    ""

                var selectedDownloadUrl =
                    ""

                var selectedVersionCode =
                    0


                /*
                 * সব Release পরীক্ষা করা হবে
                 *
                 * কারণ API-এর প্রথম Release-ই
                 * সবসময় আমাদের APK-এর সবচেয়ে বড় build
                 * নাও হতে পারে।
                 */

                for (
                    i in 0 until releases.length()
                ) {

                    val release =
                        releases.getJSONObject(i)


                    /*
                     * Draft Release বাদ
                     */

                    val draft =
                        release.optBoolean(
                            "draft",
                            false
                        )


                    if (draft) {
                        continue
                    }


                    /*
                     * Tag
                     */

                    val tagName =
                        release.optString(
                            "tag_name",
                            ""
                        ).trim()


                    if (tagName.isEmpty()) {
                        continue
                    }


                    /*
                     * APK Assets
                     */

                    val assets =
                        release.optJSONArray(
                            "assets"
                        )


                    if (assets == null) {
                        continue
                    }


                    var foundApkUrl =
                        ""


                    /*
                     * RoyalCyberTV.apk খোঁজা
                     */

                    for (
                        j in 0 until assets.length()
                    ) {

                        val asset =
                            assets.getJSONObject(j)


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

                            foundApkUrl =
                                asset.optString(
                                    "browser_download_url",
                                    ""
                                ).trim()

                            break
                        }
                    }


                    /*
                     * APK না থাকলে এই Release বাদ
                     */

                    if (
                        foundApkUrl.isEmpty()
                    ) {

                        continue
                    }


                    /*
                     * Release Build Number
                     */

                    val releaseVersionCode =
                        extractVersionCode(
                            tagName
                        )


                    /*
                     * Version বুঝতে না পারলে বাদ
                     */

                    if (
                        releaseVersionCode <= 0
                    ) {

                        continue
                    }


                    /*
                     * =================================================
                     * সবচেয়ে বড় VERSION CODE নির্বাচন
                     * =================================================
                     *
                     * যেমন:
                     *
                     * build-99
                     * build-125
                     * build-127
                     *
                     * তাহলে build-127 নেওয়া হবে।
                     */

                    if (
                        selectedRelease == null ||
                        releaseVersionCode >
                        selectedVersionCode
                    ) {

                        selectedRelease =
                            release

                        selectedTag =
                            tagName

                        selectedName =
                            release.optString(
                                "name",
                                ""
                            ).trim()

                        selectedBody =
                            release.optString(
                                "body",
                                ""
                            ).trim()

                        selectedDownloadUrl =
                            foundApkUrl

                        selectedVersionCode =
                            releaseVersionCode
                    }
                }


                /*
                 * কোনো valid Release পাওয়া যায়নি
                 */

                if (
                    selectedRelease == null
                ) {

                    throw Exception(
                        "GitHub Release-এ $APK_NAME পাওয়া যায়নি"
                    )
                }


                if (
                    selectedDownloadUrl.isEmpty()
                ) {

                    throw Exception(
                        "$APK_NAME-এর download URL পাওয়া যায়নি"
                    )
                }


                /*
                 * Installed App Version
                 */

                val currentVersionCode =
                    getCurrentVersionCode()


                /*
                 * UI Update
                 */

                handler.post {

                    if (
                        selectedVersionCode >
                        currentVersionCode
                    ) {

                        apkUrl =
                            selectedDownloadUrl


                        /*
                         * Version display
                         */

                        versionText.text =
                            "Version ${formatVersion(selectedTag)} is now available"


                        /*
                         * Release message
                         */

                        messageText.text =
                            when {

                                selectedBody.isNotBlank() ->
                                    selectedBody

                                selectedName.isNotBlank() ->
                                    selectedName

                                else ->
                                    "New version is available with important fixes. Please update now to continue watching."
                            }


                        updateButton.text =
                            "Update Now"

                        updateButton.isEnabled =
                            true


                    } else {

    /*
     * App already latest
     *
     * কোনো Toast দেখানো হবে না।
     * সরাসরি UpdateActivity বন্ধ হয়ে MainActivity-তে যাবে।
     */

    finish()
}
                }


            } catch (
                e: Exception
            ) {

                handler.post {

                    apkUrl =
                        ""

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


    /*
     * =========================================================
     * DOWNLOAD APK
     * =========================================================
     */

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

            var connection: HttpURLConnection? =
                null


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

                connection.setRequestProperty(
                    "Accept",
                    "application/octet-stream"
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


                /*
                 * App-specific Download folder
                 */

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

                    if (
                        !downloadDirectory.mkdirs()
                    ) {

                        throw Exception(
                            "Download folder তৈরি করা যায়নি"
                        )
                    }
                }


                /*
                 * APK File
                 */

                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                if (apkFile.exists()) {

                    apkFile.delete()
                }


                /*
                 * Download
                 */

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


                /*
                 * File validation
                 */

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


    /*
     * =========================================================
     * INSTALL APK
     * =========================================================
     */

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


    /*
     * =========================================================
     * CURRENT APP VERSION CODE
     * =========================================================
     */

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
     * =========================================================
     * EXTRACT VERSION CODE
     * =========================================================
     *
     * build-127 -> 127
     * build_127 -> 127
     * build 127 -> 127
     *
     * v1.0.2 -> 2
     * 1.0.2  -> 2
     */

    private fun extractVersionCode(
        tag: String
    ): Int {

        val cleanTag =
            tag.trim()


        /*
         * build-127
         * build_127
         * build 127
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
         * v1.0.2
         * 1.0.2
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
         * অন্য Tag হলে শেষের সংখ্যা
         */

        val numberRegex =
            Regex(
                "(\\d+)"
            )


        val matches =
            numberRegex
                .findAll(
                    cleanTag
                )
                .toList()


        if (
            matches.isNotEmpty()
        ) {

            return matches
                .last()
                .value
                .toIntOrNull()
                ?: 0
        }


        return 0
    }


    /*
     * =========================================================
     * FORMAT VERSION
     * =========================================================
     */

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


        if (
            cleanTag.startsWith(
                "build_",
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


    /*
     * =========================================================
     * DESTROY
     * =========================================================
     */

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )


        executor.shutdownNow()


        super.onDestroy()
    }
}
