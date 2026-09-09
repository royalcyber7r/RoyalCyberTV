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

    signingConfigs {

        create("release") {

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

            storeFile = file(storeFilePath)

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

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer:1.5.1"
    )

    implementation(
        "androidx.media3:media3-exoplayer-hls:1.5.1"
    )

    implementation(
        "androidx.media3:media3-ui:1.5.1"
    )

    implementation(
        "androidx.recyclerview:recyclerview:1.3.2"
    )
}
