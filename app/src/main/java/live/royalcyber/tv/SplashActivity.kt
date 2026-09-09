package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashTime = 2500L

    private val handler = Handler(Looper.getMainLooper())

    private val splashRunnable = Runnable {

        if (isFinishing || isDestroyed) {
            return@Runnable
        }

        try {

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)

            finish()

        } catch (_: Exception) {
            finish()
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_splash
        )

        handler.postDelayed(
            splashRunnable,
            splashTime
        )
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            splashRunnable
        )

        super.onDestroy()
    }
}
