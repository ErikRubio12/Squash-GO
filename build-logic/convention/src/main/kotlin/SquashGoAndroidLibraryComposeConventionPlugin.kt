import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SquashGoAndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("squashgo.android.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            dependencies {
                add("implementation", libs.findLibrary("compose-runtime").get())
                add("implementation", libs.findLibrary("compose-foundation").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-material-iconsCore").get())
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-uiToolingPreview").get())
                add("debugImplementation", libs.findLibrary("compose-uiTooling").get())
            }
        }
    }
}
