// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
        classpath ("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
    }
}
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.android.library") version "8.7.0" apply false
}