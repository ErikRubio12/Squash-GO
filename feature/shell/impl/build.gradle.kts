plugins {
    id("squashgo.android.feature")
}

android {
    namespace = "com.egr.squashgo.feature.shell.impl"
}

dependencies {
    implementation(projects.feature.shell.api)
    implementation(projects.feature.discover.api)
    implementation(projects.feature.play.api)
    implementation(projects.feature.activity.api)
    implementation(projects.feature.profile.api)
}
