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
         * =========================================================
         * GITHUB RELEASES API
         * =========================================================
         */

        private const val GITHUB_API =
            "https://api.github.com/repos/royalcyber7r/RoyalCyberTV/releases?per_page=100"


        /*
         * পুরোনো APK নাম
         *
         * RoyalCyberTV.apk
         */

        private const val APK_NAME =
            "RoyalCyberTV.apk"


        /*
         * বর্তমান GitHub Actions Release APK
         *
         * যেমন:
         *
         * RoyalCyberTV-1.0.145.apk
         */

        private const val APK_PREFIX =
            "RoyalCyberTV-"


        private const val APK_EXTENSION =
            ".apk"
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


        /*
         * =========================================================
         * UPDATE BUTTON
         * =========================================================
         */

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


        /*
         * =========================================================
         * LATER BUTTON
         * =========================================================
         */

        laterText.setOnClickListener {

            finish()
        }


        /*
         * =========================================================
         * START UPDATE CHECK
         * =========================================================
         */

        checkLatestRelease()
    }


    /*
     * =========================================================
     * CHECK LATEST RELEASE
     * =========================================================
     */

    private fun checkLatestRelease() {

        if (isFinishing || isDestroyed) {
            return
        }


        updateButton.isEnabled =
            false

        updateButton.text =
            "Checking Update..."


        executor.execute {

            var connection: HttpURLConnection? =
                null


            try {

                /*
                 * =================================================
                 * CONNECT GITHUB
                 * =================================================
                 */

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
                 * =================================================
                 * GITHUB API HEADERS
                 * =================================================
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


                /*
                 * =================================================
                 * RESPONSE CODE
                 * =================================================
                 */

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


                /*
                 * =================================================
                 * READ RESPONSE
                 * =================================================
                 */

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
                 * =================================================
                 * JSON ARRAY
                 * =================================================
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
                 * SELECT LATEST RELEASE
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
                 * =================================================
                 * CHECK ALL RELEASES
                 * =================================================
                 */

                for (
                    i in 0 until releases.length()
                ) {

                    val release =
                        releases.getJSONObject(i)


                    /*
                     * =================================================
                     * DRAFT RELEASE SKIP
                     * =================================================
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
                     * =================================================
                     * PRERELEASE SKIP
                     *
                     * Beta / Alpha Release বাদ
                     * =================================================
                     */

                    val prerelease =
                        release.optBoolean(
                            "prerelease",
                            false
                        )


                    if (prerelease) {
                        continue
                    }


                    /*
                     * =================================================
                     * TAG NAME
                     * =================================================
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
                     * =================================================
                     * RELEASE ASSETS
                     * =================================================
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


                    var foundApkName =
                        ""


                    /*
                     * =================================================
                     * SEARCH APK
                     *
                     * Support:
                     *
                     * RoyalCyberTV.apk
                     *
                     * RoyalCyberTV-1.0.145.apk
                     *
                     * RoyalCyberTV-145.apk
                     *
                     * =================================================
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


                        val browserDownloadUrl =
                            asset.optString(
                                "browser_download_url",
                                ""
                            ).trim()


                        if (
                            assetName.isEmpty() ||
                            browserDownloadUrl.isEmpty()
                        ) {
                            continue
                        }


                        /*
                         * Exact old APK name
                         */

                        if (
                            assetName.equals(
                                APK_NAME,
                                ignoreCase = true
                            )
                        ) {

                            foundApkName =
                                assetName


                            foundApkUrl =
                                browserDownloadUrl


                            break
                        }


                        /*
                         * New GitHub Actions APK name
                         *
                         * RoyalCyberTV-1.0.145.apk
                         */

                        val lowerName =
                            assetName.lowercase()


                        if (
                            lowerName.startsWith(
                                APK_PREFIX.lowercase()
                            ) &&
                            lowerName.endsWith(
                                APK_EXTENSION.lowercase()
                            )
                        ) {

                            foundApkName =
                                assetName


                            foundApkUrl =
                                browserDownloadUrl
                        }
                    }


                    /*
                     * =================================================
                     * APK না থাকলে Release বাদ
                     * =================================================
                     */

                    if (
                        foundApkUrl.isEmpty()
                    ) {

                        continue
                    }


                    /*
                     * =================================================
                     * EXTRACT VERSION CODE
                     * =================================================
                     */

                    val releaseVersionCode =
                        extractVersionCode(
                            tagName
                        )


                    /*
                     * =================================================
                     * VERSION INVALID হলে বাদ
                     * =================================================
                     */

                    if (
                        releaseVersionCode <= 0
                    ) {

                        continue
                    }


                    /*
                     * =================================================
                     * SELECT HIGHEST VERSION
                     * =================================================
                     *
                     * Example:
                     *
                     * v1.0.140
                     * v1.0.141
                     * v1.0.144
                     * v1.0.145
                     *
                     * তাহলে 145 নির্বাচন হবে।
                     * =================================================
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
                 * =================================================
                 * NO VALID RELEASE
                 * =================================================
                 */

                if (
                    selectedRelease == null
                ) {

                    throw Exception(
                        "GitHub Release-এ RoyalCyberTV APK পাওয়া যায়নি"
                    )
                }


                if (
                    selectedDownloadUrl.isEmpty()
                ) {

                    throw Exception(
                        "APK-এর download URL পাওয়া যায়নি"
                    )
                }


                /*
                 * =================================================
                 * CURRENT INSTALLED VERSION
                 * =================================================
                 */

                val currentVersionCode =
                    getCurrentVersionCode()


                /*
                 * =================================================
                 * UPDATE UI
                 * =================================================
                 */

                handler.post {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@post
                    }


                    /*
                     * =================================================
                     * NEW VERSION AVAILABLE
                     * =================================================
                     */

                    if (
                        selectedVersionCode >
                        currentVersionCode
                    ) {

                        apkUrl =
                            selectedDownloadUrl


                        /*
                         * Version
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


                        /*
                         * Update button
                         */

                        updateButton.text =
                            "Update Now"


                        updateButton.isEnabled =
                            true


                    } else {

                        /*
                         * =================================================
                         * ALREADY LATEST
                         * =================================================
                         *
                         * কোনো Toast নয়।
                         *
                         * UpdateActivity বন্ধ হবে।
                         * =================================================
                         */

                        finish()
                    }
                }


            } catch (
                e: Exception
            ) {

                handler.post {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@post
                    }


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
         * =========================================================
         * ANDROID 8+ UNKNOWN SOURCES PERMISSION
         * =========================================================
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


        /*
         * =========================================================
         * START DOWNLOAD
         * =========================================================
         */

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

                /*
                 * =================================================
                 * CONNECT APK URL
                 * =================================================
                 */

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


                /*
                 * =================================================
                 * HEADERS
                 * =================================================
                 */

                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV Android App"
                )


                connection.setRequestProperty(
                    "Accept",
                    "application/octet-stream"
                )


                connection.connect()


                /*
                 * =================================================
                 * RESPONSE
                 * =================================================
                 */

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


                /*
                 * =================================================
                 * TOTAL SIZE
                 * =================================================
                 */

                val totalBytes =
                    connection.contentLengthLong


                /*
                 * =================================================
                 * APP-SPECIFIC DOWNLOAD DIRECTORY
                 * =================================================
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
                 * =================================================
                 * APK FILE
                 * =================================================
                 */

                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                /*
                 * পুরোনো APK delete
                 */

                if (apkFile.exists()) {

                    apkFile.delete()
                }


                /*
                 * =================================================
                 * DOWNLOAD
                 * =================================================
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


                            /*
                             * =================================================
                             * UPDATE PROGRESS
                             * =================================================
                             */

                            if (
                                totalBytes > 0
                            ) {

                                val percent =
                                    (
                                        downloaded *
                                            100L /
                                            totalBytes
                                    )
                                        .toInt()
                                        .coerceIn(
                                            0,
                                            100
                                        )


                                handler.post {

                                    if (
                                        !isFinishing &&
                                        !isDestroyed
                                    ) {

                                        progressBar.progress =
                                            percent


                                        progressText.text =
                                            "$percent %"
                                    }
                                }
                            }
                        }


                        output.flush()
                    }
                }


                /*
                 * =================================================
                 * FILE VALIDATION
                 * =================================================
                 */

                if (
                    !apkFile.exists() ||
                    apkFile.length() <= 0
                ) {

                    throw Exception(
                        "Downloaded APK file invalid"
                    )
                }


                /*
                 * =================================================
                 * INSTALL
                 * =================================================
                 */

                handler.post {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@post
                    }


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

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@post
                    }


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

            /*
             * =================================================
             * FILE PROVIDER URI
             * =================================================
             */

            val apkUri =
                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    apkFile
                )


            /*
             * =================================================
             * INSTALL INTENT
             * =================================================
             */

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


            startActivity(
                intent
            )


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
     * GET CURRENT APP VERSION CODE
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
     * Supported:
     *
     * build-127 -> 127
     * build_127 -> 127
     * build 127 -> 127
     *
     * v1.0.2 -> 2
     * 1.0.2 -> 2
     *
     * v1.0.145 -> 145
     * =========================================================
     */

    private fun extractVersionCode(
        tag: String
    ): Int {

        val cleanTag =
            tag.trim()


        /*
         * =========================================================
         * BUILD NUMBER
         * =========================================================
         */

        val buildRegex =
            Regex(
                "(?i)build[-_ ]?(\\d+)"
            )


        val buildMatch =
            buildRegex.find(
                cleanTag
            )


        if (
            buildMatch != null
        ) {

            return buildMatch
                .groupValues[1]
                .toIntOrNull()
                ?: 0
        }


        /*
         * =========================================================
         * SEMANTIC VERSION
         *
         * v1.0.145
         * 1.0.145
         * =========================================================
         */

        val versionRegex =
            Regex(
                "^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)"
            )


        val versionMatch =
            versionRegex.find(
                cleanTag
            )


        if (
            versionMatch != null
        ) {

            return versionMatch
                .groupValues[3]
                .toIntOrNull()
                ?: 0
        }


        /*
         * =========================================================
         * FALLBACK LAST NUMBER
         * =========================================================
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


        /*
         * build-145
         */

        if (
            cleanTag.startsWith(
                "build-",
                ignoreCase = true
            )
        ) {

            return cleanTag
        }


        /*
         * build_145
         */

        if (
            cleanTag.startsWith(
                "build_",
                ignoreCase = true
            )
        ) {

            return cleanTag
        }


        /*
         * build 145
         */

        if (
            cleanTag.startsWith(
                "build ",
                ignoreCase = true
            )
        ) {

            return cleanTag
        }


        /*
         * v1.0.145
         *
         * Display:
         *
         * 1.0.145
         */

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
