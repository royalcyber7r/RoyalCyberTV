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

        /*
         * GitHub Release-এর APK filename
         *
         * GitHub Release asset-এর নাম অবশ্যই:
         * RoyalCyberTV.apk
         */
        private const val APK_NAME =
            "RoyalCyberTV.apk"

        /*
         * Temporary download file
         */
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


    @Volatile
    private var isChecking = false

    @Volatile
    private var isDownloading = false


    /*
     * Android 8+ unknown source permission
     */
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

            if (isChecking || isDownloading) {
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

                /*
                 * URL না থাকলে আবার check করবে না।
                 * Activity reload হওয়ার প্রয়োজন নেই।
                 */
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

        if (isChecking || isDownloading) {
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


                var selectedTag =
                    ""

                var selectedApkUrl =
                    ""


                // -------------------------------------------------
                // FIND RELEASE APK
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

                        laterText.isEnabled =
                            true

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

        if (
            isDownloading ||
            isChecking
        ) {
            return
        }


        isDownloading =
            true


        titleText.text =
            "Downloading Update"


        versionText.text =
            latestVersionName?.let {
                "Version $it"
            } ?: "Downloading..."


        messageText.text =
            "APK download হচ্ছে...\nঅনুগ্রহ করে অপেক্ষা করুন।"


        progressBar.progress =
            0

        percentText.text =
            "0 %"


        thread {

            var connection:
                HttpURLConnection? = null

            var outputStream:
                FileOutputStream? = null

            var inputStream:
                java.io.InputStream? = null


            try {

                // -------------------------------------------------
                // DOWNLOAD DIRECTORY
                // -------------------------------------------------

                val downloadDirectory =
                    getExternalFilesDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS
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
                        !downloadDirectory.mkdirs() &&
                        !downloadDirectory.exists()
                    ) {

                        throw Exception(
                            "Download folder তৈরি করা যায়নি"
                        )
                    }
                }


                // -------------------------------------------------
                // FINAL APK
                // -------------------------------------------------

                val apkFile =
                    File(
                        downloadDirectory,
                        APK_NAME
                    )


                // -------------------------------------------------
                // TEMP APK
                // -------------------------------------------------

                val tempFile =
                    File(
                        downloadDirectory,
                        TEMP_APK_NAME
                    )


                /*
                 * আগের incomplete download delete
                 */
                try {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                } catch (_: Exception) {
                }


                /*
                 * আগের APK delete
                 */
                try {
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }
                } catch (_: Exception) {
                }


                // -------------------------------------------------
                // OPEN GITHUB DOWNLOAD URL
                // -------------------------------------------------

                val url =
                    URL(apkUrl)


                connection =
                    url.openConnection()
                        as HttpURLConnection


                /*
                 * সবচেয়ে গুরুত্বপূর্ণ পরিবর্তন:
                 *
                 * GitHub Release asset-এর redirect
                 * automatic follow করবে।
                 */
                connection!!.instanceFollowRedirects =
                    true


                connection!!.requestMethod =
                    "GET"


                connection!!.connectTimeout =
                    30000


                connection!!.readTimeout =
                    60000


                connection!!.useCaches =
                    false


                connection!!.setRequestProperty(
                    "User-Agent",
                    "RoyalCyberTV"
                )


                connection!!.setRequestProperty(
                    "Accept",
                    "application/octet-stream"
                )


                connection!!.setRequestProperty(
                    "Connection",
                    "keep-alive"
                )


                // -------------------------------------------------
                // RESPONSE
                // -------------------------------------------------

                val responseCode =
                    connection!!.responseCode


                if (
                    responseCode !in 200..299
                ) {

                    throw Exception(
                        "Download HTTP $responseCode"
                    )
                }


                // -------------------------------------------------
                // FILE SIZE
                // -------------------------------------------------

                val totalBytes =
                    connection!!.contentLengthLong


                // -------------------------------------------------
                // INPUT STREAM
                // -------------------------------------------------

                inputStream =
                    connection!!.inputStream


                // -------------------------------------------------
                // TEMP OUTPUT
                // -------------------------------------------------

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


                // -------------------------------------------------
                // DOWNLOAD LOOP
                // -------------------------------------------------

                while (true) {

                    val count =
                        inputStream!!.read(
                            buffer
                        )


                    if (count == -1) {
                        break
                    }


                    if (count == 0) {
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
                                downloadedBytes *
                                    100L /
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


                // -------------------------------------------------
                // CLOSE STREAMS
                // -------------------------------------------------

                inputStream!!.close()
                inputStream = null


                outputStream!!.flush()
                outputStream!!.close()
                outputStream = null


                connection!!.disconnect()
                connection = null


                // -------------------------------------------------
                // VERIFY TEMP FILE
                // -------------------------------------------------

                if (
                    !tempFile.exists() ||
                    tempFile.length() <= 0
                ) {

                    throw Exception(
                        "APK download সম্পূর্ণ হয়নি"
                    )
                }


                /*
                 * APK-এর minimum size check।
                 *
                 * খুব ছোট file হলে সাধারণত HTML/error page
                 * download হয়েছে।
                 */
                if (
                    tempFile.length() < 100 * 1024
                ) {

                    throw Exception(
                        "সঠিক APK পাওয়া যায়নি"
                    )
                }


                // -------------------------------------------------
                // RENAME TEMP -> APK
                // -------------------------------------------------

                if (
                    apkFile.exists()
                ) {

                    apkFile.delete()
                }


                val renamed =
                    tempFile.renameTo(
                        apkFile
                    )


                if (!renamed) {

                    /*
                     * rename ব্যর্থ হলে copy করে নেওয়া।
                     */
                    tempFile.copyTo(
                        apkFile,
                        overwrite = true
                    )

                    tempFile.delete()
                }


                // -------------------------------------------------
                // FINAL VERIFY
                // -------------------------------------------------

                if (
                    !apkFile.exists() ||
                    apkFile.length() <= 0
                ) {

                    throw Exception(
                        "APK file তৈরি হয়নি"
                    )
                }


                // -------------------------------------------------
                // DOWNLOAD COMPLETE
                // -------------------------------------------------

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
                    inputStream?.close()
                } catch (_: Exception) {
                }


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
                        "Download error: ${
                            e.message ?: "Unknown error"
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


        // -----------------------------------------------------
        // ANDROID 8+
        // UNKNOWN SOURCE PERMISSION
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


                    versionText.text =
                        latestVersionName?.let {
                            "Version $it"
                        } ?: ""


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
                "Install শুরু করা যাচ্ছে না।\n\n${
                    e.message ?: "Unknown error"
                }"


            Toast.makeText(
                this,
                "Install error: ${
                    e.message ?: "Unknown"
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


        /*
         * Android 8+ permission settings থেকে ফিরে এলে
         * automatic install হবে।
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

    override fun onDestroy() {

        isChecking =
            false

        super.onDestroy()
    }
}
