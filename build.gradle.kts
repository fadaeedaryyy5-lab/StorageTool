plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "app.storagetool"
    compileSdk = 34
    defaultConfig {
        applicationId = "app.storagetool"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("dev.mobile:dadb:1.2.7")
}
