plugins { id("com.android.application") }

android {
    namespace = "com.musab.dosyayoneticisi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.musab.dosyayoneticisi"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
}
