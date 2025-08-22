plugins {
    id("com.android.application") version "8.7.0"
    id("org.jetbrains.kotlin.android") version "2.0.21"
}

android {
    namespace = "com.hyphenate.callkit.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hyphenate.callkit.demo"
        minSdk = 24
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        // 启用buildConfig以获得更好的IDE支持
        buildConfig = true
    }
    packaging {
        jniLibs {
            pickFirsts.add("lib/x86/libaosl.so")
            pickFirsts.add("lib/x86_64/libaosl.so")
            pickFirsts.add("lib/armeabi-v7a/libaosl.so")
            pickFirsts.add("lib/arm64-v8a/libaosl.so")
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":ease-call-kit"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}