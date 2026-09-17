package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDelay = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({

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

        }, splashDelay)
    }
}
