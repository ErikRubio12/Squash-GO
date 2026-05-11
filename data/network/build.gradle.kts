plugins {
    id("squashgo.kmp.library")
}

android {
    namespace = "com.egr.squashgo.data.network"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
            implementation(projects.core.domain)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            // BrandPalette + ThemeTokens live here. Required by BrandPaletteMapper and
            // SupabaseRemoteThemeSource, which produce Compose Color values and therefore
            // can only exist on the Android source set.
            implementation(projects.core.designsystem)
            // Compose UI is needed for `androidx.compose.ui.graphics.Color`, which the
            // mapper parses hex strings into. Only the type is used (no @Composable code),
            // so no Compose plugin is required here.
            implementation(libs.compose.ui)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
