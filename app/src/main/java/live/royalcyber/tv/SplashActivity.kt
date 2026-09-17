package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDelay = 2500L
    private val handler = Handler(Looper.getMainLooper())

    private val openMainActivity = Runnable {
        try {
            startActivity(
                Intent(this@SplashActivity, MainActivity::class.java)
            )
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        handler.postDelayed(openMainActivity, splashDelay)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMainActivity)
        super.onDestroy()
    }
}
