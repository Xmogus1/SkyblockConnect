/**
 * Precompiled [noammaddons.publishing.gradle.kts][Noammaddons_publishing_gradle] script plugin.
 *
 * @see Noammaddons_publishing_gradle
 */
public
class Noammaddons_publishingPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Noammaddons_publishing_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
