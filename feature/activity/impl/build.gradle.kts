plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.activity.impl"
}

dependencies {
    implementation(projects.feature.activity.api)
    implementation(projects.core.domain)
}
