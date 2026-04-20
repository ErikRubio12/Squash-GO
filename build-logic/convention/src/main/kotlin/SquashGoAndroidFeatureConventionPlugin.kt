import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class SquashGoAndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("squashgo.android.library.compose")
            pluginManager.apply("squashgo.hilt")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:navigation"))
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodelCompose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtimeCompose").get())
                add("implementation", libs.findLibrary("navigation-compose").get())
            }
        }
    }
}
