// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false // Adding KSP plugin with a version
}

buildscript {
    dependencies {
        classpath(libs.google.services) // Add Google services classpath here
        classpath("com.apollographql.apollo3:apollo-gradle-plugin:3.7.3")
    }
}