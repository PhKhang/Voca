dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("androidx.core:core:1.12.0")
    implementation("androidx.media:media:1.7.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:chromecast-sender:0.30")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
}

    plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.voca"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.voca"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

