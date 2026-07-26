val preprocessLegit = registerPreprocessTask("legit")
val preprocessCheat = registerPreprocessTask("cheat")
val preprocessCurseforge = registerPreprocessTask("curseforge")

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    dependsOn(preprocessLegit)
    exclude("fabric.mod.json5", "sbc.mixins.json5")

    from(layout.buildDirectory.dir("preprocessed/legit/resources")) {
        include("fabric.mod.json", "sbc.mixins.json")
    }

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

fun registerVariantResources(variantName: String, preprocessTask: TaskProvider<Task>) {
    val capitalized = variantName.replaceFirstChar { it.uppercase() }
    val taskName = "process${capitalized}Resources"

    if (tasks.findByName(taskName) == null) tasks.register<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "sbc.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "sbc.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
    else tasks.named<ProcessResources>(taskName) {
        inputs.property("version", project.version)
        dependsOn(preprocessTask)
        exclude("fabric.mod.json5", "sbc.mixins.json5")

        from(layout.buildDirectory.dir("preprocessed/$variantName/resources")) {
            include("fabric.mod.json", "sbc.mixins.json")
        }

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
}

registerVariantResources("legit", preprocessLegit)
registerVariantResources("cheat", preprocessCheat)
registerVariantResources("curseforge", preprocessCurseforge)