plugins {
    id("squashgo.android.library.compose")
}

android {
    namespace = "com.egr.squashgo.core.ui"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
}
