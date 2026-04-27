plugins {
    id("squashgo.android.library")
    id("squashgo.hilt")
}

android {
    namespace = "com.egr.squashgo.data.network.di"
}

dependencies {
    implementation(projects.data.network)
    implementation(projects.core.config)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
}
