plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.dachkaboiz.betterbudget_bestbudget"
    compileSdk=36

    defaultConfig {
        applicationId = "com.dachkaboiz.betterbudget_bestbudget"
        minSdk = 26
        targetSdk = 36
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

        buildFeatures {
            viewBinding = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures{
        viewBinding=true;
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.emoji2.bundled)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Emoji
    implementation("androidx.emoji2:emoji2-emojipicker:1.5.0")
    implementation("androidx.emoji2:emoji2-bundled:1.5.0")
}
