package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mobile only
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
