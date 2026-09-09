package live.royalcyber.tv

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

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
}
