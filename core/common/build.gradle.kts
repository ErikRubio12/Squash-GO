plugins {
    id("squashgo.android.library")
    id("squashgo.hilt")
}

android {
    namespace = "com.egr.squashgo.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
