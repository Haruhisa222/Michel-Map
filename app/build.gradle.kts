// build.gradle.kts の一番上(6.25)
import java.util.Properties

// ルートの .env を読み込んで props に詰める(6.25)
val props = Properties().apply {
    rootProject.file(".env").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    // ← ここから
    buildFeatures {
        // BuildConfig クラスの自動生成を有効にする
        buildConfig = true
    }
    // ← ここまでを追加6.25
    namespace = "com.example.myapplication6"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication6"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPS_API_KEY", "\"${props.getProperty("MAPS_API_KEY","")}\"")//6.25
        manifestPlaceholders["MAPS_API_KEY"] = props.getProperty("MAPS_API_KEY","")//6.25

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
