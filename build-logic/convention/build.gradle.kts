plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.plugins.androidApplication.toDep())
    compileOnly(libs.plugins.androidLibrary.toDep())
    compileOnly(libs.plugins.kotlinMultiplatform.toDep())
    compileOnly(libs.plugins.composeMultiplatform.toDep())
    compileOnly(libs.plugins.composeCompiler.toDep())
    compileOnly(libs.plugins.ksp.toDep())
    compileOnly(libs.plugins.hilt.toDep())
    compileOnly(libs.plugins.kotlinx.serialization.toDep())
}

fun Provider<PluginDependency>.toDep() =
    map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "squashgo.kmp.library"
            implementationClass = "SquashGoKmpLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "squashgo.android.library"
            implementationClass = "SquashGoAndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "squashgo.android.library.compose"
            implementationClass = "SquashGoAndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "squashgo.android.feature"
            implementationClass = "SquashGoAndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "squashgo.android.application"
            implementationClass = "SquashGoAndroidApplicationConventionPlugin"
        }
        register("hilt") {
            id = "squashgo.hilt"
            implementationClass = "SquashGoHiltConventionPlugin"
        }
    }
}
