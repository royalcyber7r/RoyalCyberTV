package live.royalcyber.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

import org.json.JSONArray

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UpdateActivity : AppCompatActivity() {

    companion object {

        private const val RELEASES_API =
            "https://api.github.com/repos/royalcyber7r/RoyalCyberTV/releases?per_page=100"

        private const val APK_NAME =
            "RoyalCyberTV.apk"

        private const val APK_PREFIX =
            "RoyalCyberTV-"
    }

    private lateinit var titleText: TextView
    private lateinit var messageText: TextView
    private lateinit var updateButton: Button
    private lateinit var laterButton: Button

    private var latestApkUrl: String? = null
    private var latestVersionName: String? = null

    @Volatile
    private var isChecking = false

    @Volatile
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_update)

        titleText = findViewById(R.id.update_title)
        messageText = findViewById(R.id.update_message)
        updateButton = findViewById(R.id.update_button)
        laterButton = findViewById(R.id.later_button)

        updateButton.setOnClickListener {

            if (isDownloading) {
                return@setOnClickListener
            }

            val apkUrl = latestApkUrl

            if (apkUrl.isNullOrBlank()) {

                Toast.makeText(
                    this,
                    "Update পাওয়া যায়নি",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            downloadAndInstall(apkUrl)
        }

        laterButton.setOnClickListener {
            finish()
        }

        updateButton.isEnabled = false

        checkLatestRelease()
    }

    // =========================================================
    // CHECK LATEST RELEASE
    // =========================================================

    private fun checkLatestRelease() {

        if (isChecking) {
            return
        }

        isChecking = true

        messageText.text =
            "নতুন Version খোঁজা হচ্ছে..."

        thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(RELEASES_API)

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.useCaches = false

                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.github+json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV"
                )

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception("HTTP $responseCode")
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val releases = JSONArray(response)

                var selectedTag = ""
                var selectedApkUrl = ""

                /*
                 * GitHub API সাধারণত newest release আগে দেয়।
                 * Draft / prerelease বাদ দেওয়া হচ্ছে।
                 */
                for (i in 0 until releases.length()) {

                    val release =
                        releases.optJSONObject(i)
                            ?: continue

                    val draft =
                        release.optBoolean("draft", false)

                    val prerelease =
                        release.optBoolean("prerelease", false)

                    if (draft || prerelease) {
                        continue
                    }

                    val tagName =
                        release.optString("tag_name")
                            .trim()

                    if (tagName.isEmpty()) {
                        continue
                    }

                    val assets =
                        release.optJSONArray("assets")
                            ?: continue

                    for (j in 0 until assets.length()) {

                        val asset =
                            assets.optJSONObject(j)
                                ?: continue

                        val assetName =
                            asset.optString("name")
                                .trim()

                        val browserDownloadUrl =
                            asset.optString(
                                "browser_download_url"
                            ).trim()

                        if (
                            assetName.equals(
                                APK_NAME,
                                ignoreCase = true
                            ) &&
                            browserDownloadUrl.isNotEmpty()
                        ) {

                            selectedTag = tagName
                            selectedApkUrl =
                                browserDownloadUrl

                            break
                        }
                    }

                    if (selectedApkUrl.isNotEmpty()) {
                        break
                    }
                }

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    isChecking = false

                    if (selectedApkUrl.isEmpty()) {

                        titleText.text =
                            "No Update"

                        messageText.text =
                            "বর্তমানে কোনো নতুন APK পাওয়া যায়নি।"

                        updateButton.isEnabled = false

                        return@runOnUiThread
                    }

                    latestApkUrl = selectedApkUrl
                    latestVersionName = selectedTag

                    titleText.text =
                        "New Update is Available"

                    messageText.text =
                        "Version $selectedTag পাওয়া গেছে।\n\nUpdate Now চাপুন।"

                    updateButton.isEnabled = true
                }

            } catch (e: Exception) {

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    isChecking = false

                    titleText.text =
                        "Update Check Failed"

                    messageText.text =
                        "Update check করা যাচ্ছে না।\n\nইন্টারনেট সংযোগ পরীক্ষা করুন।"

                    updateButton.isEnabled = false
                }

            } finally {

                try {
                    connection?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    // =========================================================
    // DOWNLOAD + INSTALL
    // =========================================================

    private fun downloadAndInstall(apkUrl: String) {

        if (isDownloading) {
            return
        }

        isDownloading = true

        updateButton.isEnabled = false
        laterButton.isEnabled = false

        titleText.text =
            "Downloading Update"

        messageText.text =
            "APK download হচ্ছে...\nঅনুগ্রহ করে অপেক্ষা করুন।"

        thread {

            var connection: HttpURLConnection? = null
            var apkFile: File? = null

            try {

                val cacheDirectory =
                    externalCacheDir ?: cacheDir

                /*
                 * আগের downloaded APK delete
                 */
                cacheDirectory.listFiles()
                    ?.forEach { file ->

                        if (
                            file.name.startsWith(APK_PREFIX) ||
                            file.name.equals(
                                APK_NAME,
                                ignoreCase = true
                            )
                        ) {

                            try {
                                file.delete()
                            } catch (_: Exception) {
                            }
                        }
                    }

                apkFile =
                    File(
                        cacheDirectory,
                        APK_NAME
                    )

                val url = URL(apkUrl)

                connection =
                    url.openConnection()
                        as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.useCaches = false
                connection.instanceFollowRedirects = true

                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV"
                )

                val responseCode =
                    connection.responseCode

                if (responseCode !in 200..299) {
                    throw Exception(
                        "Download HTTP $responseCode"
                    )
                }

                val contentLength =
                    connection.contentLengthLong

                connection.inputStream.use { input ->

                    FileOutputStream(
                        apkFile
                    ).use { output ->

                        val buffer =
                            ByteArray(8192)

                        var total = 0L

                        while (true) {

                            val count =
                                input.read(buffer)

                            if (count == -1) {
                                break
                            }

                            output.write(
                                buffer,
                                0,
                                count
                            )

                            total += count

                            if (contentLength > 0) {

                                val progress =
                                    (
                                        total * 100L /
                                            contentLength
                                        ).toInt()

                                runOnUiThread {

                                    if (
                                        !isFinishing &&
                                        !isDestroyed
                                    ) {

                                        messageText.text =
                                            "APK download হচ্ছে...\n\n$progress%"
                                    }
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
                        "Downloaded APK is empty"
                    )
                }

                val downloadedFile = apkFile

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    messageText.text =
                        "Download সম্পন্ন হয়েছে।\nInstall শুরু হচ্ছে..."

                    installApk(downloadedFile)
                }

            } catch (e: Exception) {

                runOnUiThread {

                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    isDownloading = false

                    updateButton.isEnabled =
                        latestApkUrl != null

                    laterButton.isEnabled = true

                    titleText.text =
                        "Update Failed"

                    messageText.text =
                        "APK download করা যায়নি।\n\nআবার চেষ্টা করুন।"

                    Toast.makeText(
                        this,
                        "Update download failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                try {
                    connection?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    // =========================================================
    // INSTALL APK
    // =========================================================

    private fun installApk(apkFile: File) {

        try {

            /*
             * Android 8+
             * Unknown Sources permission
             */
            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
            ) {

                val canInstall =
                    packageManager
                        .canRequestPackageInstalls()

                if (!canInstall) {

                    val settingsIntent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                        )

                    settingsIntent.data =
                        Uri.parse(
                            "package:$packageName"
                        )

                    startActivity(settingsIntent)

                    Toast.makeText(
                        this,
                        "Install permission Allow করে আবার Update চাপুন",
                        Toast.LENGTH_LONG
                    ).show()

                    isDownloading = false

                    updateButton.isEnabled = true
                    laterButton.isEnabled = true

                    return
                }
            }

            /*
             * FileProvider URI
             */
            val apkUri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    apkFile
                )

            val installIntent =
                Intent(Intent.ACTION_VIEW)

            installIntent.setDataAndType(
                apkUri,
                "application/vnd.android.package-archive"
            )

            installIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            installIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            try {

                startActivity(installIntent)

                isDownloading = true

            } catch (e: ActivityNotFoundException) {

                isDownloading = false

                updateButton.isEnabled = true
                laterButton.isEnabled = true

                Toast.makeText(
                    this,
                    "APK Installer পাওয়া যাচ্ছে না",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            isDownloading = false

            updateButton.isEnabled = true
            laterButton.isEnabled = true

            Toast.makeText(
                this,
                "Install শুরু করা যাচ্ছে না",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        /*
         * Unknown Sources settings থেকে ফিরে এলে
         * Update button আবার চালু থাকবে।
         */
        if (
            isDownloading &&
            latestApkUrl != null
        ) {

            updateButton.isEnabled = true
            laterButton.isEnabled = true
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        isChecking = false

        super.onDestroy()
    }
}
