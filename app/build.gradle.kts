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
    implementation(libs.recyclerview)
    implementation(libs.activity)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
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

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.android.material:material:1.13.0-alpha11")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation("io.getstream:stream-video-android-ui-compose:1.5.0")
    implementation("io.getstream:stream-webrtc-android:1.3.8")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation ("com.google.firebase:firebase-messaging:23.0.8")
    implementation(libs.firebase.messaging)
}

    plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

