plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.hyphenate.callkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1") // 请检查最新版本
    // Coil: load image library
    implementation("io.coil-kt:coil:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Refresh layout
    implementation("io.github.scwang90:refresh-layout-kernel:3.0.0-alpha")
    implementation("io.github.scwang90:refresh-header-material:3.0.0-alpha")

    // 集成 lite SDK
    api ("io.agora.rtc:lite-sdk:4.5.1")

    api ("io.hyphenate:hyphenate-chat:4.16.0")
//    implementation(project(mapOf("path" to ":hyphenatechatsdk")))
}

//apply {from("../script/maven-push-release.gradle")}