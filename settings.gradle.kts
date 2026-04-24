rootProject.name = "SquashGo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":app")

// Core modules
include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:designsystem")
include(":core:navigation")
include(":core:auth")
include(":core:ui")

// Data modules
include(":data:network")
include(":data:database")

// Feature modules
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:discover:api")
include(":feature:discover:impl")
include(":feature:play:api")
include(":feature:play:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:activity:api")
include(":feature:activity:impl")
include(":feature:shell:api")
include(":feature:shell:impl")