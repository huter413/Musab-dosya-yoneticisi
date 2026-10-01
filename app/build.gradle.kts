plugins { id("com.android.application") }

android {
    namespace = "com.musab.dosyayoneticisi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.musab.dosyayoneticisi"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
    }

    flavorDimensions += "product"
    productFlavors {
        create("manager") {
            dimension = "product"
            applicationId = "com.musab.dosyayoneticisi"
            versionName = "1.2.0"
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
}
