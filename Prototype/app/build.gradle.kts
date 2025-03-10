plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.prototypes.prototype"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.prototypes.prototype"
        minSdk = 23
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

    buildFeatures.dataBinding = true
    buildFeatures.viewBinding = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.com.google.firebase.firebase.auth)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.android.maps.utils)
    implementation(libs.google.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.common)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)

    implementation(libs.activity.ktx)
    implementation(libs.camera.core)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.androidx.camera.camera2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


}