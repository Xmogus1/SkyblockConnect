/**
 * Precompiled [noammaddons.variants.gradle.kts][Noammaddons_variants_gradle] script plugin.
 *
 * @see Noammaddons_variants_gradle
 */
public
class Noammaddons_variantsPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Noammaddons_variants_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
