import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kaiser0733.g102controller"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kaiser0733.g102controller"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "2.2.0"
    }

    signingConfigs {
        // Pinned debug keystore: every CI run signs with the SAME key so future APKs
        // install directly over this version — no delete/reinstall ever again.
        // (v1 artifacts were runner-throwaway-signed; that required one last uninstall.)
        create("pinned") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("pinned")
        }
        release {
            isMinifyEnabled = false
            // Optional production signing, wired by CI from GitHub Actions
            // secrets (see RELEASE_SIGNING.md). Absent env vars -> unsigned
            // release artifact; never a silent debug-key fallback.
            val ksFile = System.getenv("ANDROID_KEYSTORE_FILE")
            val ksPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val ksAlias = System.getenv("ANDROID_KEY_ALIAS")
            val ksKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            if (ksFile != null && ksPassword != null && ksAlias != null &&
                ksKeyPassword != null && rootProject.file(ksFile).exists()
            ) {
                signingConfig = signingConfigs.create("production") {
                    storeFile = rootProject.file(ksFile)
                    storePassword = ksPassword
                    keyAlias = ksAlias
                    keyPassword = ksKeyPassword
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Zero runtime dependencies by design (DECISIONS.md D6). JUnit4 for the pure-logic tests.
    testImplementation("junit:junit:4.13.2")
}
