plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "cn.tr1ck.matrixclock"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.tr1ck.matrixclock"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val signingStoreFile = System.getenv("SIGNING_STORE_FILE")
                ?: (project.findProperty("SIGNING_STORE_FILE") as String?)
                ?: "release.keystore"
            storeFile = file(signingStoreFile)
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: (project.findProperty("SIGNING_STORE_PASSWORD") as String?)
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: (project.findProperty("SIGNING_KEY_ALIAS") as String?)
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: (project.findProperty("SIGNING_KEY_PASSWORD") as String?)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.nanohttpd)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}