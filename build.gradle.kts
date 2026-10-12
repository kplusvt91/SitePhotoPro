plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }


android {
    namespace = "vn.sitephotopro"
    compileSdk = 35

    defaultConfig {
        applicationId = "vn.sitephotopro"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinOptions {
    jvmTarget = "17"
}
