plugins {
    id("squashgo.kmp.library")
}

android {
    namespace = "com.egr.squashgo.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
        }
    }
}
