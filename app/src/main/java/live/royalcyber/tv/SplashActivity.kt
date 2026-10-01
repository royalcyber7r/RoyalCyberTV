package live.royalcyber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDelay = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android TV হলে কোনো Splash দেখাবে না
        if (isAndroidTV()) {

            try {

                startActivity(
                    Intent(
                        this,
                        TvMainActivity::class.java
                    )
                )

            } catch (_: Exception) {
            }

            finish()
            return
        }

        // Mobile-এর আগের Splash একই থাকবে
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            try {

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

                finish()

            } catch (_: Exception) {

                finish()
            }

        }, splashDelay)
    }

    private fun isAndroidTV(): Boolean {

        return packageManager.hasSystemFeature(
            PackageManager.FEATURE_LEANBACK
        )
    }
}
