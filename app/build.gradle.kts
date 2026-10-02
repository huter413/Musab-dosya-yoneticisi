plugins { id("com.android.application") }

android {
    namespace = "com.musab.dosyayoneticisi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.musab.dosyayoneticisi"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"
    }

    flavorDimensions += "product"
    productFlavors {
        create("manager") {
            dimension = "product"
            applicationId = "com.musab.dosyayoneticisi"
            versionName = "1.2.1"
        }
        create("skkLoader") {
            dimension = "product"
            applicationId = "com.musab.skkinstaller"
            versionCode = 1
            versionName = "1.0.0"
        }
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("com.android.tools.build:apksig:8.8.0")
}
