package com.skyblockconnect.websocket

import com.google.gson.JsonObject
import com.skyblockconnect.SkyblockConnect.logger
import com.skyblockconnect.SkyblockConnect.mc
import com.skyblockconnect.features.impl.general.Connection
import com.skyblockconnect.utils.ThreadUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object SbcSocket {
    private const val RETRY_DELAY_MS = 30_000L

    private val executor = Executors.newSingleThreadExecutor { Thread(it, "SBC-WebSocket").apply { isDaemon = true } }
    private val http: HttpClient = HttpClient.newBuilder().executor(executor).build()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var wanted = false
    private val connecting = AtomicBoolean(false)

    @Volatile var host: String? = null
        private set

    val connected get() = socket != null

    fun start() {
        wanted = true
        connect()
    }

    fun stop() {
        wanted = false
        runCatching { socket?.sendClose(WebSocket.NORMAL_CLOSURE, "bye") }
        socket = null
        host = null
    }

    fun reconnect() {
        runCatching { socket?.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect") }
        socket = null
        wanted = true
        connect()
    }

    fun send(json: JsonObject): Boolean {
        val ws = socket ?: return false
        return runCatching { ws.sendText(json.toString(), true) }.isSuccess
    }

    private fun connect() {
        if (! wanted || ! connecting.compareAndSet(false, true)) return

        val url = Connection.serverUrl.value.trim()
        val uri = runCatching { URI.create(url) }.getOrNull()
        if (uri == null || (uri.scheme != "ws" && uri.scheme != "wss")) {
            connecting.set(false)
            ThreadUtils.runOnMcThread { Connection.onConnectFailed("invalid server address \"$url\"") }
            return
        }

        http.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(uri, SocketListener())
            .whenComplete { ws, err ->
                connecting.set(false)
                if (ws == null) {
                    val why = err?.cause?.message ?: err?.message ?: "unknown error"
                    logger.info("SBC: connect to $url failed ($why), retrying in ${RETRY_DELAY_MS / 1000}s")
                    ThreadUtils.runOnMcThread { Connection.onConnectFailed(why) }
                    scheduleRetry()
                }
                else {
                    socket = ws
                    host = uri.host
                    sendHello()
                    ThreadUtils.runOnMcThread { Connection.onConnected(uri.host) }
                }
            }
    }

    private fun sendHello() = runCatching {
        val user = mc.user
        send(JsonObject().apply {
            addProperty("type", "hello")
            addProperty("uuid", user.profileId.toString())
            addProperty("name", user.name)
        })
    }

    private fun scheduleRetry() {
        if (! wanted) return
        ThreadUtils.setTimeout(RETRY_DELAY_MS) { if (wanted && socket == null) connect() }
    }

    private class SocketListener: WebSocket.Listener {

        private val buffer = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            buffer.append(data)
            if (last) {
                val message = buffer.toString()
                buffer.setLength(0)
                ThreadUtils.runOnMcThread { SbcNet.receive(message) }
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            onDrop("closed ($statusCode)")
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            onDrop(error.message ?: error.javaClass.simpleName)
        }

        private fun onDrop(why: String) {
            if (socket != null) {
                logger.info("SBC: disconnected - $why")
                ThreadUtils.runOnMcThread { Connection.onDisconnected(why) }
            }
            socket = null
            host = null
            scheduleRetry()
        }
    }
}
