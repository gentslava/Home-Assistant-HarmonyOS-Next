plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Huawei AppGallery Connect — enable for Phase 1e (Wear Engine). Needs agconnect-services.json.
    // id("com.huawei.agconnect")
}

android {
    namespace = "ru.gentslava.homeassistant.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.gentslava.homeassistant.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // HA REST client + JSON
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Secure storage for HA URL + token
    implementation(libs.androidx.security.crypto)

    // Wear Engine — enable in Phase 1e (also uncomment the agconnect plugin + add agconnect-services.json)
    // implementation(libs.huawei.wearengine)

    testImplementation(libs.junit)
}
