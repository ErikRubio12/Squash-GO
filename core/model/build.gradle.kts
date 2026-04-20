plugins {
    id("squashgo.kmp.library")
}

android {
    namespace = "com.egr.squashgo.core.model"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
    }
}
