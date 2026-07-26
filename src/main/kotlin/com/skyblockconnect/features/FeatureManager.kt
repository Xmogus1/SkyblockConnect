package com.skyblockconnect.features

import com.skyblockconnect.SkyblockConnect
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.config.Config
import com.skyblockconnect.event.EventBus.register
import com.skyblockconnect.event.impl.RenderOverlayEvent
import com.skyblockconnect.ui.clickgui.enums.CategoryType
import com.skyblockconnect.ui.hud.HudEditorScreen
import com.skyblockconnect.ui.hud.HudElement
import com.skyblockconnect.ui.utils.Resolution
import com.skyblockconnect.utils.render.Render2D.width
import io.github.classgraph.ClassGraph

object FeatureManager {
    val hudElements = mutableListOf<HudElement>()
    val features = mutableSetOf<Feature>()

    fun registerFeatures() {
        val scanResult = ClassGraph()
            .acceptPackages("com.skyblockconnect")
            .ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scanResult.use { result ->
            val featureClasses = result.getSubclasses("com.skyblockconnect.features.Feature")
            SkyblockConnect.logger.debug("ClassGraph found ${featureClasses.size} subclasses of Feature")

            featureClasses.forEach { classInfo ->

                if (classInfo.isAbstract) return@forEach

                try {
                    val clazz = classInfo.loadClass()
                    val instance = clazz.getDeclaredField("INSTANCE").get(null) as? Feature

                    instance?.let { feature ->
                        feature.initialize()
                        hudElements.addAll(feature.hudElements)
                        features.add(feature)
                        SkyblockConnect.logger.info("Successfully loaded feature: ${feature::class.simpleName}")
                    }
                }
                catch (e: Exception) {
                    SkyblockConnect.logger.error("Failed to load feature class: ${classInfo.name}", e)
                }
            }
        }

        Config.load()

        register<RenderOverlayEvent> {
            if (mc.screen == HudEditorScreen) return@register

            Resolution.refresh()
            Resolution.push(event.context)
            hudElements.forEach { if (it.shouldDraw) it.renderElement(event.context, false) }
            Resolution.pop(event.context)
        }
    }

    fun getFeaturesByCategory(category: CategoryType): List<Feature> {
        return features.filter { it.category == category }
    }

    fun getFeatureByName(name: String): Feature? {
        return features.find { it.name == name }
    }

    fun getHudByName(name: String): HudElement? {
        return hudElements.find { it.name == name }
    }

    fun createFeatureList(): String {
        val featureList = StringBuilder()
        for ((category, features) in features.groupBy { it.category }.entries.sortedBy { it.key.ordinal }) {
            featureList.appendLine("Category: ${category.name}")
            for (feature in features.sortedByDescending { it.name.width() }) {
                featureList.appendLine("- ${feature.name}: ${feature.description ?: ""}")
            }
            featureList.appendLine()
        }
        return featureList.toString()
    }
}
