/**
 * Precompiled [noammaddons.loom.gradle.kts][Noammaddons_loom_gradle] script plugin.
 *
 * @see Noammaddons_loom_gradle
 */
public
class Noammaddons_loomPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Noammaddons_loom_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
