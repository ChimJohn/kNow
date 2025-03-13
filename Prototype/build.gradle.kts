// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
buildscript {
    dependencies {
        // Make sure this is set to a compatible AGP version (8.7.3 is recommended).
        classpath("com.android.tools.build:gradle:8.8.0")
    }
}