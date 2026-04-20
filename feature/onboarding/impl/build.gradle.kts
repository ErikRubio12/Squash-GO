plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.onboarding.impl"
}

dependencies {
    implementation(projects.feature.onboarding.api)
    implementation(projects.core.domain)
    implementation(projects.core.auth)
}
