package live.royalcyber.tv

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
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


    private var downloadId: Long = -1L

    private var waitingForInstallPermission = false

    private var pendingInstallFile: File? = null

    private var receiverRegistered = false


    // =========================================================
    // DOWNLOAD RECEIVER
    // =========================================================

    private val downloadReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action !=
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE
                ) {
                    return
                }


                val completedId =
                    intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID,
                        -1L
                    )


                if (
                    completedId !=
                    downloadId
                ) {
                    return
                }


                checkDownloadResult()
            }
        }


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
        // REGISTER DOWNLOAD RECEIVER
        // -----------------------------------------------------

        registerDownloadReceiver()


        // -----------------------------------------------------
        // UPDATE BUTTON
        // -----------------------------------------------------

        updateButton.setOnClickListener {

            /*
             * দ্বিতীয়বার download শুরু হতে দেওয়া হবে না।
             */
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


            /*
             * Button সঙ্গে সঙ্গে disable।
             */
            updateButton.isEnabled = false
            laterText.isEnabled = false


            startDownload(
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
    // REGISTER DOWNLOAD RECEIVER
    // =========================================================

    @Suppress("DEPRECATION")
    private fun registerDownloadReceiver() {

        if (receiverRegistered) {
            return
        }


        try {

            val filter =
                IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE
                )


            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                registerReceiver(
                    downloadReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )

            } else {

                registerReceiver(
                    downloadReceiver,
                    filter
                )
            }


            receiverRegistered = true

        } catch (_: Exception) {

            receiverRegistered = false
        }
    }


    // =========================================================
    // CHECK LATEST GITHUB RELEASE
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


        updateButton.isEnabled =
            false


        progressBar.progress =
            0

        percentText.text =
            "0 %"


        thread {

            var connection:
                HttpURLConnection? = null


            try {

                val url =
                    URL(RELEASES_API)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                connection.requestMethod =
                    "GET"


                connection.connectTimeout =
                    15000


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
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }


                val releases =
                    JSONArray(response)


                var selectedTag =
                    ""

                var selectedApkUrl =
                    ""


                // -------------------------------------------------
                // FIND FIRST VALID RELEASE
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


                    if (
                        draft ||
                        prerelease
                    ) {
                        continue
                    }


                    val tagName =
                        release.optString(
                            "tag_name"
                        ).trim()


                    if (
                        tagName.isEmpty()
                    ) {
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

                            selectedTag =
                                tagName

                            selectedApkUrl =
                                browserDownloadUrl

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


                    isChecking =
                        false


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


                        progressBar.progress =
                            0


                        percentText.text =
                            "0 %"


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


                    isChecking =
                        false


                    titleText.text =
                        "Update Check Failed"


                    versionText.text =
                        "Unable to check for update"


                    messageText.text =
                        "Update check করা যাচ্ছে না।\n\nইন্টারনেট সংযোগ পরীক্ষা করুন।"


                    updateButton.isEnabled =
                        false


                    progressBar.progress =
                        0


                    percentText.text =
                        "0 %"
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
    // START DOWNLOAD
    // =========================================================

    private fun startDownload(
        apkUrl: String
    ) {

        if (isDownloading) {
            return
        }


        isDownloading =
            true


        titleText.text =
            "Downloading Update"


        versionText.text =
            latestVersionName?.let {
                "Version $it"
            }
                ?: "Downloading..."


        messageText.text =
            "APK download হচ্ছে...\nঅনুগ্রহ করে অপেক্ষা করুন।"


        progressBar.progress =
            0


        percentText.text =
            "0 %"


        try {

            // -------------------------------------------------
            // OLD APK DELETE
            // -------------------------------------------------

            val downloadDirectory =
                getExternalFilesDir(
                    Environment.DIRECTORY_DOWNLOADS
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


            downloadDirectory.listFiles()
                ?.forEach { file ->

                    try {

                        if (
                            file.name.equals(
                                APK_NAME,
                                ignoreCase = true
                            ) ||
                            file.name.startsWith(
                                APK_PREFIX
                            )
                        ) {

                            file.delete()
                        }

                    } catch (_: Exception) {
                    }
                }


            // -------------------------------------------------
            // DOWNLOAD MANAGER
            // -------------------------------------------------

            val request =
                DownloadManager.Request(
                    Uri.parse(apkUrl)
                )


            request.setTitle(
                "RoyalCyberTV Update"
            )


            request.setDescription(
                "RoyalCyberTV $latestVersionName download হচ্ছে..."
            )


            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE
            )


            request.setAllowedOverMetered(
                true
            )


            request.setAllowedOverRoaming(
                true
            )


            request.setMimeType(
                "application/vnd.android.package-archive"
            )


            /*
             * আপনার file_paths.xml-এ
             * external-files-path আছে।
             *
             * তাই এই location FileProvider-এর
             * মাধ্যমে Install করা যাবে।
             */
            request.setDestinationInExternalFilesDir(
                this,
                Environment.DIRECTORY_DOWNLOADS,
                APK_NAME
            )


            val downloadManager =
                getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager


            downloadId =
                downloadManager.enqueue(
                    request
                )


            /*
             * DownloadManager enqueue সফল হয়েছে।
             */
            messageText.text =
                "APK download শুরু হয়েছে...\n\nঅনুগ্রহ করে অপেক্ষা করুন।"


            /*
             * Progress দেখানোর জন্য polling শুরু।
             */
            startDownloadProgressMonitoring()


        } catch (e: Exception) {

            isDownloading =
                false


            updateButton.isEnabled =
                true


            laterText.isEnabled =
                true


            titleText.text =
                "Download Failed"


            messageText.text =
                "APK download শুরু করা যায়নি।"


            Toast.makeText(
                this,
                "Download শুরু হয়নি: ${e.message ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // DOWNLOAD PROGRESS MONITOR
    // =========================================================

    private fun startDownloadProgressMonitoring() {

        thread {

            while (
                isDownloading &&
                downloadId != -1L &&
                !isFinishing
            ) {

                try {

                    val manager =
                        getSystemService(
                            Context.DOWNLOAD_SERVICE
                        ) as DownloadManager


                    val query =
                        DownloadManager.Query()
                            .setFilterById(
                                downloadId
                            )


                    val cursor:
                        Cursor? =
                        manager.query(query)


                    cursor?.use {

                        if (it.moveToFirst()) {

                            val statusIndex =
                                it.getColumnIndex(
                                    DownloadManager.COLUMN_STATUS
                                )


                            val downloadedIndex =
                                it.getColumnIndex(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )


                            val totalIndex =
                                it.getColumnIndex(
                                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                )


                            val status =
                                if (
                                    statusIndex >= 0
                                ) {
                                    it.getInt(
                                        statusIndex
                                    )
                                } else {
                                    -1
                                }


                            val downloaded =
                                if (
                                    downloadedIndex >= 0
                                ) {
                                    it.getLong(
                                        downloadedIndex
                                    )
                                } else {
                                    0L
                                }


                            val total =
                                if (
                                    totalIndex >= 0
                                ) {
                                    it.getLong(
                                        totalIndex
                                    )
                                } else {
                                    0L
                                }


                            if (
                                total > 0
                            ) {

                                val progress =
                                    (
                                        downloaded * 100L /
                                            total
                                        )
                                        .toInt()
                                        .coerceIn(
                                            0,
                                            100
                                        )


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


                            /*
                             * ERROR হলে receiver আসার আগেই
                             * UI-তে জানিয়ে দেওয়া।
                             */
                            if (
                                status ==
                                DownloadManager.STATUS_FAILED
                            ) {

                                runOnUiThread {

                                    handleDownloadFailed()
                                }

                                return@thread
                            }
                        }
                    }


                    Thread.sleep(
                        500
                    )

                } catch (_: Exception) {

                    break
                }
            }
        }
    }


    // =========================================================
    // DOWNLOAD COMPLETE
    // =========================================================

    private fun checkDownloadResult() {

        if (
            downloadId == -1L
        ) {
            return
        }


        try {

            val manager =
                getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager


            val query =
                DownloadManager.Query()
                    .setFilterById(
                        downloadId
                    )


            val cursor =
                manager.query(
                    query
                )


            cursor?.use {

                if (!it.moveToFirst()) {
                    return
                }


                val statusIndex =
                    it.getColumnIndex(
                        DownloadManager.COLUMN_STATUS
                    )


                if (
                    statusIndex < 0
                ) {
                    return
                }


                val status =
                    it.getInt(
                        statusIndex
                    )


                when (status) {

                    DownloadManager.STATUS_SUCCESSFUL -> {

                        val file =
                            File(
                                getExternalFilesDir(
                                    Environment.DIRECTORY_DOWNLOADS
                                ),
                                APK_NAME
                            )


                        if (
                            !file.exists() ||
                            file.length() <= 0
                        ) {

                            handleDownloadFailed()
                            return
                        }


                        isDownloading =
                            false


                        progressBar.progress =
                            100


                        percentText.text =
                            "100 %"


                        titleText.text =
                            "Download Complete"


                        messageText.text =
                            "Download সম্পন্ন হয়েছে।\n\nInstall শুরু হচ্ছে..."


                        pendingInstallFile =
                            file


                        installApk(
                            file
                        )
                    }


                    DownloadManager.STATUS_FAILED -> {

                        handleDownloadFailed()
                    }
                }
            }

        } catch (e: Exception) {

            handleDownloadFailed()
        }
    }


    // =========================================================
    // DOWNLOAD FAILED
    // =========================================================

    private fun handleDownloadFailed() {

        isDownloading =
            false


        updateButton.isEnabled =
            latestApkUrl != null


        laterText.isEnabled =
            true


        titleText.text =
            "Download Failed"


        versionText.text =
            "Download failed"


        messageText.text =
            "APK download করা যায়নি।\n\nআবার Update চাপুন।"


        progressBar.progress =
            0


        percentText.text =
            "0 %"


        Toast.makeText(
            this,
            "APK download failed",
            Toast.LENGTH_LONG
        ).show()
    }


    // =========================================================
    // INSTALL APK
    // =========================================================

    private fun installApk(
        apkFile: File
    ) {

        if (
            !apkFile.exists()
        ) {

            handleDownloadFailed()
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

                    val settingsIntent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                        )


                    settingsIntent.data =
                        Uri.parse(
                            "package:$packageName"
                        )


                    startActivity(
                        settingsIntent
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


                    isDownloading =
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

            isDownloading =
                false


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

            isDownloading =
                false


            updateButton.isEnabled =
                true


            laterText.isEnabled =
                true


            messageText.text =
                "Install শুরু করা যাচ্ছে না।\n\n${e.message ?: "Unknown error"}"


            Toast.makeText(
                this,
                "Install error",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()


        /*
         * Unknown Sources settings থেকে ফিরে এসেছে।
         */
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

    @Suppress("DEPRECATION")
    override fun onDestroy() {

        isChecking =
            false


        /*
         * DownloadManager-এর download
         * Activity destroy হলেও চলতে পারে।
         */
        try {

            if (receiverRegistered) {

                unregisterReceiver(
                    downloadReceiver
                )

                receiverRegistered =
                    false
            }

        } catch (_: Exception) {
        }


        super.onDestroy()
    }
}
