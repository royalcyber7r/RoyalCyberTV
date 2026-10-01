package live.royalcyber.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
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
    }


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var titleText: TextView
    private lateinit var versionText: TextView
    private lateinit var messageText: TextView
    private lateinit var updateButton: Button
    private lateinit var laterText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var percentText: TextView


    // =========================================================
    // UPDATE DATA
    // =========================================================

    private var latestApkUrl: String? = null
    private var latestVersionName: String? = null


    @Volatile
    private var isChecking = false


    @Volatile
    private var isDownloading = false


    private var waitingForInstallPermission = false

    private var pendingInstallFile: File? = null


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_update
        )


        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------

        titleText =
            findViewById(
                R.id.update_title
            )

        versionText =
            findViewById(
                R.id.update_version
            )

        messageText =
            findViewById(
                R.id.update_message
            )

        progressBar =
            findViewById(
                R.id.update_progress
            )

        percentText =
            findViewById(
                R.id.update_percent
            )

        updateButton =
            findViewById(
                R.id.update_button
            )

        laterText =
            findViewById(
                R.id.update_later
            )


        // -----------------------------------------------------
        // INITIAL STATE
        // -----------------------------------------------------

        progressBar.max = 100
        progressBar.progress = 0

        percentText.text = "0 %"

        updateButton.isEnabled = false
        laterText.isEnabled = true


        // -----------------------------------------------------
        // UPDATE BUTTON
        // -----------------------------------------------------

        updateButton.setOnClickListener {

            if (isDownloading) {
                return@setOnClickListener
            }


            val apkUrl =
                latestApkUrl


            if (apkUrl.isNullOrBlank()) {

                Toast.makeText(
                    this,
                    "Update পাওয়া যায়নি",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            updateButton.isEnabled = false
            laterText.isEnabled = false


            startApkDownload(
                apkUrl
            )
        }


        // -----------------------------------------------------
        // LATER
        // -----------------------------------------------------

        laterText.setOnClickListener {

            if (isDownloading) {
                return@setOnClickListener
            }

            finish()
        }


        // -----------------------------------------------------
        // CHECK UPDATE
        // -----------------------------------------------------

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


        titleText.text =
            "Checking for Update"

        versionText.text =
            "Please wait..."

        messageText.text =
            "নতুন Version খোঁজা হচ্ছে..."


        updateButton.isEnabled = false


        thread {

            var connection: HttpURLConnection? = null


            try {

                val url =
                    URL(RELEASES_API)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                connection.requestMethod = "GET"

                connection.connectTimeout = 15000

                connection.readTimeout = 20000

                connection.useCaches = false

                connection.instanceFollowRedirects = true


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

                    throw Exception(
                        "GitHub HTTP $responseCode"
                    )
                }


                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                val releases =
                    JSONArray(response)


                var selectedTag = ""

                var selectedApkUrl = ""


                // -------------------------------------------------
                // FIND VALID RELEASE
                // -------------------------------------------------

                for (
                    i in 0 until releases.length()
                ) {

                    val release =
                        releases.optJSONObject(i)
                            ?: continue


                    val draft =
                        release.optBoolean(
                            "draft",
                            false
                        )


                    val prerelease =
                        release.optBoolean(
                            "prerelease",
                            false
                        )


                    if (draft || prerelease) {
                        continue
                    }


                    val tagName =
                        release.optString(
                            "tag_name"
                        ).trim()


                    if (tagName.isEmpty()) {
                        continue
                    }


                    val assets =
                        release.optJSONArray(
                            "assets"
                        )
                            ?: continue


                    for (
                        j in 0 until assets.length()
                    ) {

                        val asset =
                            assets.optJSONObject(j)
                                ?: continue


                        val assetName =
                            asset.optString(
                                "name"
                            ).trim()


                        val browserUrl =
                            asset.optString(
                                "browser_download_url"
                            ).trim()


                        if (
                            assetName.equals(
                                APK_NAME,
                                ignoreCase = true
                            ) &&
                            browserUrl.isNotEmpty()
                        ) {

                            selectedTag =
                                tagName

                            selectedApkUrl =
                                browserUrl

                            break
                        }
                    }


                    if (
                        selectedApkUrl.isNotEmpty()
                    ) {
                        break
                    }
                }


                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    isChecking = false


                    if (
                        selectedApkUrl.isEmpty()
                    ) {

                        titleText.text =
                            "No Update"


                        versionText.text =
                            "No new version available"


                        messageText.text =
                            "বর্তমানে কোনো নতুন APK পাওয়া যায়নি।"


                        updateButton.isEnabled =
                            false


                        return@runOnUiThread
                    }


                    latestApkUrl =
                        selectedApkUrl


                    latestVersionName =
                        selectedTag


                    titleText.text =
                        "New Update is Available"


                    versionText.text =
                        "Version $selectedTag is now available"


                    messageText.text =
                        "নতুন Version পাওয়া গেছে।\n\nUpdate Now চাপুন।"


                    updateButton.isEnabled =
                        true


                    laterText.isEnabled =
                        true


                    progressBar.progress =
                        0


                    percentText.text =
                        "0 %"
                }


            } catch (e: Exception) {

                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    isChecking = false


                    titleText.text =
                        "Update Check Failed"


                    versionText.text =
                        "Unable to check for update"


                    messageText.text =
                        "Update check করা যাচ্ছে না।\n\nইন্টারনেট সংযোগ পরীক্ষা করুন।"


                    updateButton.isEnabled =
                        false
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
    // START APK DOWNLOAD
    // =========================================================

    private fun startApkDownload(
        apkUrl: String
    ) {

        if (isDownloading) {
            return
        }


        isDownloading = true


        titleText.text =
            "Downloading Update"


        versionText.text =
            latestVersionName?.let {
                "Version $it"
            } ?: "Downloading..."


        messageText.text =
            "APK download হচ্ছে...\nঅনুগ্রহ করে অপেক্ষা করুন।"


        progressBar.progress = 0

        percentText.text = "0 %"


        thread {

            var connection: HttpURLConnection? = null

            var outputStream: FileOutputStream? = null


            try {

                // -------------------------------------------------
                // DOWNLOAD DIRECTORY
                // -------------------------------------------------

                val downloadDirectory =
                    getExternalFilesDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )


                if (downloadDirectory == null) {

                    throw Exception(
                        "Download folder পাওয়া যায়নি"
                    )
                }


                if (!downloadDirectory.exists()) {

                    downloadDirectory.mkdirs()
                }


                // -------------------------------------------------
                // OLD APK DELETE
                // -------------------------------------------------

                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                if (apkFile.exists()) {

                    try {
                        apkFile.delete()
                    } catch (_: Exception) {
                    }
                }


                // -------------------------------------------------
                // OPEN CONNECTION
                // -------------------------------------------------

                var currentUrl =
                    apkUrl


                var redirectCount = 0


                while (true) {

                    val url =
                        URL(currentUrl)


                    connection =
                        url.openConnection()
                            as HttpURLConnection


                    connection!!.requestMethod =
                        "GET"


                    connection!!.connectTimeout =
                        20000


                    connection!!.readTimeout =
                        30000


                    connection!!.useCaches =
                        false


                    connection!!.instanceFollowRedirects =
                        false


                    connection!!.setRequestProperty(
                        "User-Agent",
                        "RoyalCyberTV"
                    )


                    connection!!.setRequestProperty(
                        "Accept",
                        "*/*"
                    )


                    val responseCode =
                        connection!!.responseCode


                    if (
                        responseCode == 301 ||
                        responseCode == 302 ||
                        responseCode == 303 ||
                        responseCode == 307 ||
                        responseCode == 308
                    ) {

                        val location =
                            connection!!.getHeaderField(
                                "Location"
                            )


                        connection!!.disconnect()


                        if (
                            location.isNullOrBlank()
                        ) {

                            throw Exception(
                                "Download redirect পাওয়া যায়নি"
                            )
                        }


                        redirectCount++


                        if (redirectCount > 10) {

                            throw Exception(
                                "Too many redirects"
                            )
                        }


                        currentUrl =
                            location


                        continue
                    }


                    if (
                        responseCode !in 200..299
                    ) {

                        throw Exception(
                            "Download HTTP $responseCode"
                        )
                    }


                    break
                }


                // -------------------------------------------------
                // FILE SIZE
                // -------------------------------------------------

                val totalBytes =
                    connection!!.contentLengthLong


                // -------------------------------------------------
                // DOWNLOAD
                // -------------------------------------------------

                val inputStream =
                    connection!!.inputStream


                outputStream =
                    FileOutputStream(
                        apkFile
                    )


                val buffer =
                    ByteArray(64 * 1024)


                var downloadedBytes =
                    0L


                var lastProgress =
                    -1


                while (true) {

                    val count =
                        inputStream.read(
                            buffer
                        )


                    if (count == -1) {
                        break
                    }


                    outputStream!!.write(
                        buffer,
                        0,
                        count
                    )


                    downloadedBytes += count


                    if (totalBytes > 0) {

                        val progress =
                            (
                                downloadedBytes * 100L /
                                    totalBytes
                                )
                                .toInt()
                                .coerceIn(
                                    0,
                                    100
                                )


                        if (
                            progress !=
                            lastProgress
                        ) {

                            lastProgress =
                                progress


                            runOnUiThread {

                                if (
                                    !isFinishing &&
                                    !isDestroyed
                                ) {

                                    progressBar.progress =
                                        progress


                                    percentText.text =
                                        "$progress %"


                                    messageText.text =
                                        "APK download হচ্ছে...\n\n$progress%"
                                }
                            }
                        }
                    }
                }


                inputStream.close()

                outputStream!!.flush()

                outputStream!!.close()

                outputStream = null


                connection!!.disconnect()

                connection = null


                // -------------------------------------------------
                // VERIFY APK
                // -------------------------------------------------

                if (
                    !apkFile.exists() ||
                    apkFile.length() <= 0
                ) {

                    throw Exception(
                        "APK file পাওয়া যায়নি"
                    )
                }


                isDownloading =
                    false


                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    progressBar.progress =
                        100


                    percentText.text =
                        "100 %"


                    titleText.text =
                        "Download Complete"


                    messageText.text =
                        "APK download সম্পন্ন হয়েছে।\n\nInstall শুরু হচ্ছে..."


                    pendingInstallFile =
                        apkFile


                    installApk(
                        apkFile
                    )
                }


            } catch (e: Exception) {

                try {
                    outputStream?.close()
                } catch (_: Exception) {
                }


                try {
                    connection?.disconnect()
                } catch (_: Exception) {
                }


                isDownloading =
                    false


                runOnUiThread {

                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    updateButton.isEnabled =
                        true


                    laterText.isEnabled =
                        true


                    titleText.text =
                        "Download Failed"


                    versionText.text =
                        "Download failed"


                    messageText.text =
                        "APK download করা যায়নি।"


                    progressBar.progress =
                        0


                    percentText.text =
                        "0 %"


                    Toast.makeText(
                        this,
                        "Download error: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // =========================================================
    // INSTALL APK
    // =========================================================

    private fun installApk(
        apkFile: File
    ) {

        if (!apkFile.exists()) {

            Toast.makeText(
                this,
                "APK file পাওয়া যায়নি",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // -----------------------------------------------------
        // ANDROID 8+
        // UNKNOWN SOURCES
        // -----------------------------------------------------

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val allowed =
                packageManager
                    .canRequestPackageInstalls()


            if (!allowed) {

                pendingInstallFile =
                    apkFile


                waitingForInstallPermission =
                    true


                try {

                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                        )


                    intent.data =
                        Uri.parse(
                            "package:$packageName"
                        )


                    startActivity(
                        intent
                    )


                    titleText.text =
                        "Install Permission Required"


                    messageText.text =
                        "Install permission Allow করুন।\n\nAllow করার পর এই পেজে ফিরে আসুন।"


                    Toast.makeText(
                        this,
                        "Install permission Allow করুন",
                        Toast.LENGTH_LONG
                    ).show()


                } catch (e: Exception) {

                    waitingForInstallPermission =
                        false


                    updateButton.isEnabled =
                        true


                    laterText.isEnabled =
                        true


                    Toast.makeText(
                        this,
                        "Install permission settings খোলা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }


                return
            }
        }


        // -----------------------------------------------------
        // FILE PROVIDER
        // -----------------------------------------------------

        try {

            val apkUri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    apkFile
                )


            val installIntent =
                Intent(
                    Intent.ACTION_VIEW
                )


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


            startActivity(
                installIntent
            )


            updateButton.isEnabled =
                false


            laterText.isEnabled =
                false


        } catch (
            e: ActivityNotFoundException
        ) {

            updateButton.isEnabled =
                true


            laterText.isEnabled =
                true


            Toast.makeText(
                this,
                "APK Installer পাওয়া যায়নি",
                Toast.LENGTH_LONG
            ).show()


        } catch (e: Exception) {

            updateButton.isEnabled =
                true


            laterText.isEnabled =
                true


            messageText.text =
                "Install শুরু করা যাচ্ছে না।\n\n${e.message ?: "Unknown error"}"


            Toast.makeText(
                this,
                "Install error: ${e.message ?: "Unknown"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()


        if (
            waitingForInstallPermission &&
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val allowed =
                packageManager
                    .canRequestPackageInstalls()


            if (allowed) {

                waitingForInstallPermission =
                    false


                val file =
                    pendingInstallFile


                if (
                    file != null &&
                    file.exists()
                ) {

                    installApk(
                        file
                    )
                }
            }
        }
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        isChecking = false

        super.onDestroy()
    }
}
