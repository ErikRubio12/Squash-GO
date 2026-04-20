plugins {
    id("squashgo.android.library")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.egr.squashgo.feature.profile.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
