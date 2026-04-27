plugins {
    id("squashgo.android.library")
    id("squashgo.hilt")
}

android {
    namespace = "com.egr.squashgo.core.config.android"
}

dependencies {
    api(projects.core.config)
}