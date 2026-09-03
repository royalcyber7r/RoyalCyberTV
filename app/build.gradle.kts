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

        versionCode =
            (project.findProperty("versionCode") as String?)
                ?.toIntOrNull()
                ?: 1

        versionName =
            (project.findProperty("versionName") as String?)
                ?: "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    /*
     * Release APK signing
     *
     * GitHub Actions থেকে signing properties দেওয়া হবে।
     */
    signingConfigs {
        create("release") {
            val storeFilePath =
                project.findProperty("RELEASE_STORE_FILE") as String?

            val storePasswordValue =
                project.findProperty("RELEASE_STORE_PASSWORD") as String?

            val keyAliasValue =
                project.findProperty("RELEASE_KEY_ALIAS") as String?

            val keyPasswordValue =
                project.findProperty("RELEASE_KEY_PASSWORD") as String?

            if (
                storeFilePath != null &&
                storePasswordValue != null &&
                keyAliasValue != null &&
                keyPasswordValue != null
            ) {
                storeFile =
                    file(storeFilePath)

                storePassword =
                    storePasswordValue

                keyAlias =
                    keyAliasValue

                keyPassword =
                    keyPasswordValue
            }
        }
    }

    buildTypes {

        getByName("release") {

            isMinifyEnabled = false

            signingConfig =
                signingConfigs.getByName("release")
        }

        getByName("debug") {
            isMinifyEnabled = false
        }
    }
}

dependencies {

    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
