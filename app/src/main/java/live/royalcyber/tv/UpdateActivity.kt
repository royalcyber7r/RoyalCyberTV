package live.royalcyber.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

import org.json.JSONArray

import java.io.File
import java.io.FileInputStream
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

        private const val TEMP_APK_NAME =
            "RoyalCyberTV.apk.part"
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

    private var installedVersionName: String = "0.0.0"

    @Volatile
    private var isChecking = false

    @Volatile
    private var isDownloading = false

    private var waitingForInstallPermission = false

    private var pendingInstallFile: File? = null


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_update)


        titleText =
            findViewById(R.id.update_title)

        versionText =
            findViewById(R.id.update_version)

        messageText =
            findViewById(R.id.update_message)

        progressBar =
            findViewById(R.id.update_progress)

        percentText =
            findViewById(R.id.update_percent)

        updateButton =
            findViewById(R.id.update_button)

        laterText =
            findViewById(R.id.update_later)


        progressBar.max = 100
        progressBar.progress = 0

        percentText.text = "0 %"

        updateButton.isEnabled = false
        laterText.isEnabled = true


        // =====================================================
        // DOWNLOAD BUTTON
        // =====================================================

        updateButton.setOnClickListener {

            if (isDownloading) {
                return@setOnClickListener
            }

            val apkUrl =
                latestApkUrl

            if (apkUrl.isNullOrBlank()) {

                Toast.makeText(
                    this,
                    "APK download link পাওয়া যায়নি",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }


            startApkDownload(
                apkUrl
            )
        }


        // =====================================================
        // LATER
        // =====================================================

        laterText.setOnClickListener {

            if (isDownloading) {
                return@setOnClickListener
            }

            finish()
        }


        // =====================================================
        // CHECK UPDATE
        // =====================================================

        checkLatestRelease()
    }


    // =========================================================
    // GET INSTALLED VERSION
    // =========================================================

    private fun getInstalledVersionName(): String {

        return try {

            val packageInfo =
                packageManager.getPackageInfo(
                    packageName,
                    0
                )

            packageInfo.versionName
                ?.trim()
                ?.removePrefix("v")
                ?.ifEmpty {
                    "0.0.0"
                }
                ?: "0.0.0"

        } catch (_: Exception) {

            "0.0.0"
        }
    }


    // =========================================================
    // VERSION TO NUMBER LIST
    //
    // Example:
    // v1.0.153 -> [1, 0, 153]
    // 1.0.154  -> [1, 0, 154]
    // =========================================================

    private fun normalizeVersion(
        version: String
    ): List<Int> {

        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".")
            .map {

                it
                    .takeWhile { char ->
                        char.isDigit()
                    }
                    .toIntOrNull()
                    ?: 0
            }
    }


    // =========================================================
    // CHECK WHETHER GITHUB VERSION IS NEWER
    // =========================================================

    private fun isNewerVersion(
        latestVersion: String,
        currentVersion: String
    ): Boolean {

        val latest =
            normalizeVersion(
                latestVersion
            )

        val current =
            normalizeVersion(
                currentVersion
            )


        val maxSize =
            maxOf(
                latest.size,
                current.size
            )


        for (
            i in 0 until maxSize
        ) {

            val latestPart =
                latest.getOrElse(i) {
                    0
                }

            val currentPart =
                current.getOrElse(i) {
                    0
                }


            if (
                latestPart >
                currentPart
            ) {

                return true
            }


            if (
                latestPart <
                currentPart
            ) {

                return false
            }
        }


        return false
    }


    // =========================================================
    // CHECK LATEST RELEASE
    // =========================================================

    private fun checkLatestRelease() {

        if (
            isChecking ||
            isDownloading
        ) {
            return
        }


        isChecking = true


        installedVersionName =
            getInstalledVersionName()


        titleText.text =
            "Checking for Update"

        versionText.text =
            "Current Version $installedVersionName"

        messageText.text =
            "নতুন Version খোঁজা হচ্ছে..."

        updateButton.isEnabled =
            false


        thread {

            var connection:
                    HttpURLConnection? =
                null


            try {

                val url =
                    URL(RELEASES_API)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                connection.requestMethod =
                    "GET"


                connection.connectTimeout =
                    20000

                connection.readTimeout =
                    30000

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
                    "RoyalCyberTV"
                )


                val responseCode =
                    connection.responseCode


                if (
                    responseCode !in 200..299
                ) {

                    throw Exception(
                        "GitHub HTTP $responseCode"
                    )
                }


                val response =
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                val releases =
                    JSONArray(
                        response
                    )


                var selectedTag =
                    ""

                var selectedApkUrl =
                    ""


                // =================================================
                // FIND LATEST VALID RELEASE WITH APK
                // =================================================

                for (
                    i in 0 until releases.length()
                ) {

                    val release =
                        releases.optJSONObject(i)
                            ?: continue


                    if (
                        release.optBoolean(
                            "draft",
                            false
                        )
                    ) {
                        continue
                    }


                    if (
                        release.optBoolean(
                            "prerelease",
                            false
                        )
                    ) {
                        continue
                    }


                    val tagName =
                        release
                            .optString(
                                "tag_name"
                            )
                            .trim()


                    if (
                        tagName.isEmpty()
                    ) {
                        continue
                    }


                    val assets =
                        release
                            .optJSONArray(
                                "assets"
                            )
                            ?: continue


                    for (
                        j in 0 until assets.length()
                    ) {

                        val asset =
                            assets
                                .optJSONObject(j)
                                ?: continue


                        val assetName =
                            asset
                                .optString(
                                    "name"
                                )
                                .trim()


                        val downloadUrl =
                            asset
                                .optString(
                                    "browser_download_url"
                                )
                                .trim()


                        if (
                            assetName.equals(
                                APK_NAME,
                                ignoreCase = true
                            ) &&
                            downloadUrl.isNotEmpty()
                        ) {

                            selectedTag =
                                tagName

                            selectedApkUrl =
                                downloadUrl

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

                    isChecking = false


                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    // =================================================
                    // NO APK FOUND
                    // =================================================

                    if (
                        selectedApkUrl.isEmpty()
                    ) {

                        titleText.text =
                            "No Update"

                        versionText.text =
                            "Current Version $installedVersionName"

                        messageText.text =
                            "GitHub Release-এ $APK_NAME পাওয়া যায়নি।"


                        updateButton.isEnabled =
                            false

                        laterText.isEnabled =
                            true


                        return@runOnUiThread
                    }


                    // =================================================
                    // COMPARE VERSION
                    // =================================================

                    val newerVersion =
                        isNewerVersion(
                            selectedTag,
                            installedVersionName
                        )


                    // =================================================
                    // SAME VERSION / OLDER VERSION
                    // =================================================

                    if (!newerVersion) {

                        latestVersionName =
                            selectedTag

                        latestApkUrl =
                            selectedApkUrl


                        titleText.text =
                            "No Update"


                        versionText.text =
                            "Current Version $installedVersionName"


                        messageText.text =
                            "আপনার App ইতিমধ্যে সর্বশেষ Version ব্যবহার করছে।"


                        progressBar.progress =
                            0

                        percentText.text =
                            "0 %"


                        updateButton.isEnabled =
                            false

                        laterText.isEnabled =
                            true


                        return@runOnUiThread
                    }


                    // =================================================
                    // NEW VERSION AVAILABLE
                    // =================================================

                    latestVersionName =
                        selectedTag

                    latestApkUrl =
                        selectedApkUrl


                    titleText.text =
                        "New Update is Available"


                    versionText.text =
                        "Current: $installedVersionName\nAvailable: $selectedTag"


                    messageText.text =
                        "নতুন Version পাওয়া গেছে।\n\nUpdate Now চাপুন।"


                    progressBar.progress =
                        0

                    percentText.text =
                        "0 %"


                    updateButton.isEnabled =
                        true

                    laterText.isEnabled =
                        true
                }


            } catch (e: Exception) {

                runOnUiThread {

                    isChecking = false


                    if (
                        isFinishing ||
                        isDestroyed
                    ) {
                        return@runOnUiThread
                    }


                    titleText.text =
                        "Update Check Failed"


                    versionText.text =
                        "Current Version $installedVersionName"


                    messageText.text =
                        "Update check করা যাচ্ছে না।\n\n${
                            e.message
                                ?: "Internet connection পরীক্ষা করুন।"
                        }"


                    updateButton.isEnabled =
                        false

                    laterText.isEnabled =
                        true
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


        isDownloading =
            true


        updateButton.isEnabled =
            false

        laterText.isEnabled =
            false


        titleText.text =
            "Downloading Update"


        versionText.text =
            latestVersionName?.let {
                "Version $it"
            } ?: "Downloading..."


        messageText.text =
            "APK download শুরু হচ্ছে..."


        progressBar.progress =
            0

        percentText.text =
            "0 %"


        thread {

            var connection:
                    HttpURLConnection? =
                null

            var outputStream:
                    FileOutputStream? =
                null


            try {

                // =================================================
                // DOWNLOAD DIRECTORY
                // =================================================

                val downloadDirectory =
                    getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                        ?: throw Exception(
                            "Download folder পাওয়া যায়নি"
                        )


                if (
                    !downloadDirectory.exists()
                ) {

                    if (
                        !downloadDirectory.mkdirs() &&
                        !downloadDirectory.exists()
                    ) {

                        throw Exception(
                            "Download folder তৈরি করা যায়নি"
                        )
                    }
                }


                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                val tempFile =
                    File(
                        downloadDirectory,
                        TEMP_APK_NAME
                    )


                if (
                    tempFile.exists()
                ) {
                    tempFile.delete()
                }


                if (
                    apkFile.exists()
                ) {
                    apkFile.delete()
                }


                // =================================================
                // OPEN GITHUB APK URL
                // =================================================

                val url =
                    URL(apkUrl)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                connection.requestMethod =
                    "GET"


                connection.connectTimeout =
                    30000

                connection.readTimeout =
                    120000

                connection.useCaches =
                    false

                connection.instanceFollowRedirects =
                    true


                connection.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/octet-stream"
                )


                // =================================================
                // CONNECT
                // =================================================

                val responseCode =
                    connection.responseCode


                if (
                    responseCode !in 200..299
                ) {

                    throw Exception(
                        "APK download HTTP $responseCode"
                    )
                }


                val totalBytes =
                    connection.contentLengthLong


                // =================================================
                // DOWNLOAD
                // =================================================

                connection.inputStream.use { input ->

                    outputStream =
                        FileOutputStream(
                            tempFile
                        )


                    val buffer =
                        ByteArray(
                            64 * 1024
                        )


                    var downloadedBytes =
                        0L

                    var lastProgress =
                        -1


                    while (true) {

                        val count =
                            input.read(
                                buffer
                            )


                        if (
                            count == -1
                        ) {
                            break
                        }


                        if (
                            count <= 0
                        ) {
                            continue
                        }


                        outputStream!!.write(
                            buffer,
                            0,
                            count
                        )


                        downloadedBytes +=
                            count


                        if (
                            totalBytes > 0
                        ) {

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


                    outputStream!!.flush()
                    outputStream!!.close()

                    outputStream =
                        null
                }


                connection.disconnect()
                connection =
                    null


                // =================================================
                // VERIFY FILE
                // =================================================

                if (
                    !tempFile.exists()
                ) {

                    throw Exception(
                        "APK file তৈরি হয়নি"
                    )
                }


                if (
                    tempFile.length() <= 0
                ) {

                    throw Exception(
                        "APK file empty"
                    )
                }


                // =================================================
                // VERIFY APK ZIP HEADER
                // =================================================

                val header =
                    ByteArray(2)


                FileInputStream(
                    tempFile
                ).use { input ->

                    val read =
                        input.read(
                            header
                        )


                    if (
                        read != 2 ||
                        header[0] !=
                            'P'.code.toByte() ||
                        header[1] !=
                            'K'.code.toByte()
                    ) {

                        throw Exception(
                            "Valid APK পাওয়া যায়নি"
                        )
                    }
                }


                // =================================================
                // MOVE TEMP → APK
                // =================================================

                if (
                    apkFile.exists()
                ) {
                    apkFile.delete()
                }


                if (
                    !tempFile.renameTo(
                        apkFile
                    )
                ) {

                    tempFile.copyTo(
                        apkFile,
                        overwrite = true
                    )

                    tempFile.delete()
                }


                if (
                    !apkFile.exists() ||
                    apkFile.length() <= 0
                ) {

                    throw Exception(
                        "APK file তৈরি করা যায়নি"
                    )
                }


                isDownloading =
                    false


                // =================================================
                // DOWNLOAD COMPLETE
                // =================================================

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


                    versionText.text =
                        latestVersionName?.let {
                            "Version $it"
                        } ?: ""


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


                try {

                    val tempFile =
                        File(
                            getExternalFilesDir(
                                Environment.DIRECTORY_DOWNLOADS
                            ),
                            TEMP_APK_NAME
                        )


                    if (
                        tempFile.exists()
                    ) {
                        tempFile.delete()
                    }

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
                        latestVersionName?.let {
                            "Version $it"
                        } ?: "APK download failed"


                    messageText.text =
                        "APK download করা যায়নি।\n\n${
                            e.message
                                ?: "Unknown error"
                        }"


                    progressBar.progress =
                        0

                    percentText.text =
                        "0 %"


                    Toast.makeText(
                        this,
                        "Download error: ${
                            e.message
                                ?: "Unknown"
                        }",
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

        if (
            !apkFile.exists() ||
            apkFile.length() <= 0
        ) {

            Toast.makeText(
                this,
                "APK file পাওয়া যায়নি",
                Toast.LENGTH_LONG
            ).show()


            updateButton.isEnabled =
                true

            laterText.isEnabled =
                true

            return
        }


        // =====================================================
        // ANDROID 8+
        // =====================================================

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            if (
                !packageManager
                    .canRequestPackageInstalls()
            ) {

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


                    versionText.text =
                        latestVersionName?.let {
                            "Version $it"
                        } ?: ""


                    messageText.text =
                        "Install permission-এ Allow করুন।\n\nতারপর এখানে ফিরে আসুন."


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


        // =====================================================
        // FILE PROVIDER INSTALL
        // =====================================================

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
                "Install শুরু করা যাচ্ছে না।\n\n${
                    e.message
                        ?: "Unknown error"
                }"


            Toast.makeText(
                this,
                "Install error: ${
                    e.message
                        ?: "Unknown"
                }",
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

            if (
                packageManager
                    .canRequestPackageInstalls()
            ) {

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

        isChecking =
            false

        super.onDestroy()
    }
}
