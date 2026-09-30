package live.royalcyber.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val isAndroidTV: Boolean
        get() = packageManager.hasSystemFeature(
            PackageManager.FEATURE_LEANBACK
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =========================================================
        // ANDROID TV
        // TV-তে কোনো Splash দেখাবে না।
        // সরাসরি TvMainActivity খুলবে।
        // =========================================================
        if (isAndroidTV) {
            startActivity(
                Intent(this, TvMainActivity::class.java)
            )
            finish()
            return
        }

        // =========================================================
        // MOBILE
        // Mobile-এ আগের Splash থাকবে।
        // =========================================================
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

            if (isFinishing || isDestroyed) {
                return@postDelayed
            }

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

        }, 2000L)
    }
}
