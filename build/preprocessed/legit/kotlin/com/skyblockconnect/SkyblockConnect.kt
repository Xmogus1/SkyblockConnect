package com.skyblockconnect

import com.skyblockconnect.commands.CommandManager
import com.skyblockconnect.config.PogObject
import com.skyblockconnect.event.EventDispatcher
import com.skyblockconnect.features.FeatureManager
import com.skyblockconnect.hypixel.HypixelApi
import com.skyblockconnect.party.PartyFinder
import com.skyblockconnect.ui.gui.MenuKeybind
import com.skyblockconnect.utils.*
import com.skyblockconnect.utils.render.ItemRenderer
import com.skyblockconnect.utils.render.ModRenderPipelines
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.slf4j.LoggerFactory

object SkyblockConnect: ClientModInitializer {
    const val MOD_ID = "sbc"
    val MOD_NAME by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.name }
    val MOD_VERSION by lazy { FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version.friendlyString }
    const val PREFIX = "§6§lSBC§r"

    @JvmField
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField
    val mc = Minecraft.getInstance()

    @JvmField
    val logger = LoggerFactory.getLogger(MOD_NAME)

    @JvmField
    var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())
    val debugFlags = mutableSetOf<String>()
    val isDev get() = debugFlags.contains("dev")

    var screen: Screen? = null
        set(value) {
            field = value
            if (value == null) return
            ThreadUtils.scheduledTask(1) {
                mc.setScreen(screen)
                field = null
            }
        }

    override fun onInitializeClient() {
        ModRenderPipelines.init()

        PictureInPictureRendererRegistry.register { ItemRenderer(it.bufferSource()) }

        EventDispatcher.init()
        ServerUtils.init()
        ChatUtils.init()

        FeatureManager.registerFeatures()
        CommandManager.registerAll()

        BossBarUtils.init()
        HypixelApi.init()
        PartyFinder.init()
        MenuKeybind.init()

        isLoaded = true
    }
}