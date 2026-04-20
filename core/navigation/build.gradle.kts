plugins {
    id("squashgo.android.library")
}

android {
    namespace = "com.egr.squashgo.core.navigation"
}

dependencies {
    implementation(libs.navigation.compose)
}
