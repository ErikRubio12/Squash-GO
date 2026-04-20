plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.discover.impl"
}

dependencies {
    implementation(projects.feature.discover.api)
    implementation(projects.feature.play.api)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.activity.api)
    implementation(projects.core.domain)
}
