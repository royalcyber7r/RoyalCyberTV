package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager

class SplashActivity : AppCompatActivity() {

    private val isAndroidTV: Boolean
        get() =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_LEANBACK
            )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        /*
         * Android TV:
         * Splash একদম বাদ দিয়ে সরাসরি MainActivity।
         */
        if (isAndroidTV) {

            try {

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

            } finally {

                finish()
            }

            return
        }

        /*
         * Mobile:
         * আগের Splash behaviour ঠিক থাকবে।
         */
        setContentView(
            R.layout.activity_splash
        )

        android.os.Handler(
            android.os.Looper.getMainLooper()
        ).postDelayed({

            if (
                !isFinishing &&
                !isDestroyed
            ) {

                try {

                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    )

                    finish()

                } catch (
                    _: Exception
                ) {

                    finish()
                }
            }

        }, 2000L)
    }
}
