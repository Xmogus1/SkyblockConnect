package com.skyblockconnect.features.impl.general

import com.skyblockconnect.features.Feature
import com.skyblockconnect.ui.clickgui.components.impl.ButtonSetting
import com.skyblockconnect.ui.clickgui.components.impl.TextInputSetting
import com.skyblockconnect.ui.clickgui.components.impl.ToggleSetting
import com.skyblockconnect.ui.notification.NotificationManager
import com.skyblockconnect.utils.ChatUtils
import com.skyblockconnect.websocket.SbcSocket

object Connection: Feature("Stay connected to the SBC relay so events reach other players.", toggled = true) {

    override fun toggle() = Unit

    val serverUrl by TextInputSetting("Server Address", "wss://tweaky-cosmetics.onrender.com")
        .withDescription("WebSocket address of the SBC relay (ws:// or wss://).")
        .onChange { SbcSocket.reconnect() }

    val showOwn by ToggleSetting("Echo My Own Reports", false)
        .withDescription("Also print the announcements you yourself sent. Useful for testing.")

    val quietWhenOffSkyblock by ToggleSetting("Only In SkyBlock", true)
        .withDescription("Hide incoming announcements while you are not on SkyBlock.")

    val connectionToasts by ToggleSetting("Connection Toasts", true)
        .withDescription("Show a toast when the relay connects or drops.")

    private val reconnectButton by ButtonSetting("Reconnect") {
        SbcSocket.reconnect()
        NotificationManager.push("SBC", "Reconnecting…")
    }.withDescription("Drop the current connection and dial the address above again.")

    private val statusButton by ButtonSetting("Print Status", false) {
        ChatUtils.modMessage(status())
    }.withDescription("Print the current relay status into chat.")

    fun status(): String = when {
        ! enabled -> "§cDisconnected §7(feature off)"
        SbcSocket.connected -> "§aConnected §7to §f${SbcSocket.host}"
        else -> "§eConnecting…"
    }

    override fun onEnable() {
        super.onEnable()
        SbcSocket.start()
    }

    override fun onDisable() {
        super.onDisable()
        SbcSocket.stop()
    }

    fun onConnected(host: String) {
        if (connectionToasts.value) NotificationManager.push("SBC", "Connected to $host")
    }

    fun onDisconnected(why: String) {
        if (connectionToasts.value) NotificationManager.error("SBC", "Disconnected - $why")
    }

    fun onConnectFailed(why: String) {
        if (connectionToasts.value) NotificationManager.error("SBC", "Connect failed - $why")
    }
}