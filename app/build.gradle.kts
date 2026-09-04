plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "live.royalcyber.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "live.royalcyber.tv"

        minSdk = 23
        targetSdk = 35

        /*
         * GitHub Actions থেকে:
         *
         * -PversionCode=132
         * -PversionName=1.0.132
         *
         * দেওয়া হলে সেগুলো ব্যবহার হবে।
         *
         * Local build হলে:
         * versionCode = 1
         * versionName = 1.0
         */

        versionCode =
            (project.findProperty("versionCode") as String?)
                ?.toIntOrNull()
                ?: 1

        versionName =
            (project.findProperty("versionName") as String?)
                ?: "1.0"
    }

    /*
     * Java 17
     */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    /*
     * Kotlin JVM 17
     */
    kotlinOptions {
        jvmTarget = "17"
    }

    /*
     * ==========================================
     * RELEASE SIGNING
     * ==========================================
     *
     * Keystore:
     *
     * RoyalCyberTV/
     * ├── royalcyber-release.jks
     * └── app/
     *     └── build.gradle.kts
     *
     * তাই app/build.gradle.kts থেকে
     * Root-এর keystore-এ যেতে ../ ব্যবহার করা হয়েছে।
     *
     * GitHub Actions চাইলে নিচের Gradle
     * properties দিয়েও signing তথ্য দিতে পারবে।
     */

    signingConfigs {

        create("release") {

            /*
             * GitHub Actions থেকে RELEASE_STORE_FILE
             * দেওয়া হলে সেটি ব্যবহার করবে।
             *
             * না দিলে Root-এর:
             *
             * ../royalcyber-release.jks
             *
             * ব্যবহার হবে।
             */

            val storeFilePath =
                project.findProperty(
                    "RELEASE_STORE_FILE"
                ) as String?
                    ?: "../royalcyber-release.jks"

            val storePasswordValue =
                project.findProperty(
                    "RELEASE_STORE_PASSWORD"
                ) as String?

            val keyAliasValue =
                project.findProperty(
                    "RELEASE_KEY_ALIAS"
                ) as String?

            val keyPasswordValue =
                project.findProperty(
                    "RELEASE_KEY_PASSWORD"
                ) as String?

            /*
             * Keystore file
             */
            storeFile = file(storeFilePath)

            /*
             * Password / Alias
             *
             * এগুলো GitHub Actions Secrets থেকে
             * দেওয়া হবে।
             */
            if (storePasswordValue != null) {
                storePassword = storePasswordValue
            }

            if (keyAliasValue != null) {
                keyAlias = keyAliasValue
            }

            if (keyPasswordValue != null) {
                keyPassword = keyPasswordValue
            }
        }
    }

    /*
     * ==========================================
     * BUILD TYPES
     * ==========================================
     */

    buildTypes {

        /*
         * RELEASE
         */
        getByName("release") {

            isMinifyEnabled = false

            /*
             * Release APK অবশ্যই release signing config
             * ব্যবহার করবে।
             */
            signingConfig =
                signingConfigs.getByName("release")
        }

        /*
         * DEBUG
         */
        getByName("debug") {

            isMinifyEnabled = false
        }
    }
}

/*
 * ==========================================
 * DEPENDENCIES
 * ==========================================
 */

dependencies {

    /*
     * AndroidX Core
     */
    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    /*
     * AndroidX AppCompat
     */
    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    /*
     * ==========================================
     * Media3 / ExoPlayer
     * ==========================================
     */

    implementation(
        "androidx.media3:media3-exoplayer:1.5.1"
    )

    implementation(
        "androidx.media3:media3-exoplayer-hls:1.5.1"
    )

    implementation(
        "androidx.media3:media3-ui:1.5.1"
    )

    /*
     * RecyclerView
     */
    implementation(
        "androidx.recyclerview:recyclerview:1.3.2"
    )
}
