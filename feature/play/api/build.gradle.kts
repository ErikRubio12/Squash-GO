plugins {
    id("squashgo.android.library")
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "com.egr.squashgo.feature.play.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
