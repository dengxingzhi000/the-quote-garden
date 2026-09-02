plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.dailymind"
    compileSdk = 36
    defaultConfig { minSdk = 24; targetSdk = 35 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    val bom = libs.androidx.compose.bom
    implementation(platform(bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation(libs.retrofit.core)
    implementation(libs.kotlinx.serialization.json)
}
