package live.royalcyber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    /* =========================================================
       ANDROID TV CHECK
       ========================================================= */

    private val isAndroidTV: Boolean
        get() =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_LEANBACK
            )


    /* =========================================================
       TV VIEWS
       ========================================================= */

    private lateinit var tvLogo: ImageView
    private lateinit var tvUpdateButton: TextView


    /* =========================================================
       ON CREATE
       ========================================================= */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        /*
         * =====================================================
         * ANDROID TV
         * =====================================================
         *
         * TV-তে শুধু:
         *
         * 1. Header
         * 2. Logo
         * 3. Update Button
         *
         * কোনো Player নেই
         * কোনো Channel নেই
         * কোনো JSON loading নেই
         * কোনো RecyclerView নেই
         * কোনো Notification নেই
         * =====================================================
         */

        if (isAndroidTV) {

            setContentView(
                R.layout.activity_tv_main
            )

            setupTV()

            return
        }


        /*
         * =====================================================
         * MOBILE
         * =====================================================
         *
         * IMPORTANT:
         *
         * এই simplified version-এ Mobile UI রাখা হয়নি।
         *
         * Mobile-এর আগের MainActivity code রাখতে চাইলে
         * Mobile অংশটি আগের code অনুযায়ী রাখতে হবে।
         *
         * =====================================================
         */

        setContentView(
            R.layout.activity_main
        )
    }


    /* =========================================================
       TV SETUP
       ========================================================= */

    private fun setupTV() {

        /*
         * TV Logo
         */

        tvLogo =
            findViewById(
                R.id.tv_logo
            )


        /*
         * Update Button
         */

        tvUpdateButton =
            findViewById(
                R.id.tv_update_button
            )


        /*
         * =====================================================
         * UPDATE BUTTON
         * =====================================================
         */

        tvUpdateButton.setOnClickListener {

            openUpdateScreen()
        }


        /*
         * =====================================================
         * TV REMOTE FOCUS
         * =====================================================
         */

        tvUpdateButton.isFocusable =
            true

        tvUpdateButton.isFocusableInTouchMode =
            true

        tvUpdateButton.requestFocus()
    }


    /* =========================================================
       OPEN UPDATE SCREEN
       ========================================================= */

    private fun openUpdateScreen() {

        try {

            startActivity(
                Intent(
                    this,
                    UpdateActivity::class.java
                )
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Update System চালু করা যাচ্ছে না",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /* =========================================================
       RESUME
       ========================================================= */

    override fun onResume() {

        super.onResume()


        /*
         * TV-তে আবার ফিরে এলে Update button focus থাকবে।
         */

        if (isAndroidTV) {

            if (
                ::tvUpdateButton.isInitialized
            ) {

                tvUpdateButton.post {

                    tvUpdateButton.requestFocus()
                }
            }
        }
    }


    /* =========================================================
       DESTROY
       ========================================================= */

    override fun onDestroy() {

        /*
         * TV-তে কোনো Player/Adapter/Thread নেই।
         */

        super.onDestroy()
    }
}
