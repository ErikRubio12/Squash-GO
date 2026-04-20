plugins {
    id("squashgo.android.library")
}

android {
    namespace = "com.egr.squashgo.data.database"
}

dependencies {
    implementation(projects.core.model)
}
