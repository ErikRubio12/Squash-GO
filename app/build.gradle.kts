import java.util.Properties

plugins {
    id("squashgo.android.application")
    id("squashgo.hilt")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "com.egr.squashgo"

    defaultConfig {
        applicationId = "com.egr.squashgo"
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SUPABASE_URL", "\"${localProperties["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties["SUPABASE_ANON_KEY"]}\"")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    // Core
    implementation(projects.core.common)
    implementation(projects.core.config)
    implementation(projects.core.config.android)
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.auth)

    // Data
    implementation(projects.data.network)
    implementation(projects.data.network.di)

    // Feature APIs
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.discover.api)
    implementation(projects.feature.play.api)
    implementation(projects.feature.profile.api)
    implementation(projects.feature.activity.api)
    implementation(projects.feature.shell.api)

    // Feature Impls
    implementation(projects.feature.onboarding.impl)
    implementation(projects.feature.discover.impl)
    implementation(projects.feature.play.impl)
    implementation(projects.feature.profile.impl)
    implementation(projects.feature.activity.impl)
    implementation(projects.feature.shell.impl)
}
