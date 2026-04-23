plugins {
    id("squashgo.android.library")
    id("squashgo.hilt")
}

android {
    namespace = "com.egr.squashgo.core.auth"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.androidx.security.crypto)
}
