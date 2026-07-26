package com.skyblockconnect.features.impl.general

import com.skyblockconnect.config.Config
import com.skyblockconnect.features.Feature
import com.skyblockconnect.features.FeatureManager
import com.skyblockconnect.ui.clickgui.components.impl.ButtonSetting
import com.skyblockconnect.ui.clickgui.components.impl.ColorSetting
import com.skyblockconnect.ui.clickgui.components.impl.KeybindSetting
import com.skyblockconnect.ui.clickgui.components.impl.MultiCheckboxSetting
import com.skyblockconnect.ui.clickgui.components.impl.ToggleSetting
import com.skyblockconnect.ui.notification.NotificationManager
import java.awt.Color

object Interface: Feature("menu appearance", name = "Interface", toggled = true) {

    val playClickSound by ToggleSetting("Click Sound", true)
        .withDescription("Play a click when you press something in the menu.")

    val accentColor by ColorSetting("Accent Color", Color(255, 187, 61), false)
        .withDescription("The accent colour used throughout the menu.")

    val menuKey by KeybindSetting("Open Menu Key")
        .withDescription("Key that opens the SBC menu. /sbc always works too.")

    private var resetArmedAt = 0L

    val resetButton by ButtonSetting("Reset All Settings") {
        val now = System.currentTimeMillis()
        if (now - resetArmedAt > 3000L) {

            resetArmedAt = now
            NotificationManager.error("Reset ALL settings?", "Click again within 3s to confirm")
        }
        else {
            resetArmedAt = 0L

            FeatureManager.features.forEach { feature ->
                feature.configSettings.forEach { setting ->
                    when (setting) {
                        is MultiCheckboxSetting -> setting.resetToDefaults()
                        is KeybindSetting -> {
                            setting.reset()
                            setting.isMouse = false
                        }

                        else -> setting.reset()
                    }
                }

                if (feature.enabled != feature.defaultEnabled) feature.toggle()
            }

            Config.save()
            NotificationManager.push("Settings reset", "Every feature restored to defaults")
        }
    }.withDescription("Resets EVERY feature and setting in the mod to its defaults. Click twice to confirm.")

    override fun toggle() {}
}
