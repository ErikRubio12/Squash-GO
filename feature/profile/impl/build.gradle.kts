plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.profile.impl"
}

dependencies {
    implementation(projects.feature.profile.api)
    implementation(projects.core.domain)
    implementation(projects.core.auth)
}
