plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.play.impl"
}

dependencies {
    implementation(projects.feature.play.api)
    implementation(projects.core.domain)
}
