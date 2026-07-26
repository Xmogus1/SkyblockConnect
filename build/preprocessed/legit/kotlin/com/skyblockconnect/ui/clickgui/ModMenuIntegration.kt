package com.skyblockconnect.ui.clickgui

import com.skyblockconnect.ui.gui.SbcScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

@Suppress("unused")
class ModMenuIntegration: ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->

            SbcScreen.modMenuParent = parent
            SbcScreen
        }
    }
}